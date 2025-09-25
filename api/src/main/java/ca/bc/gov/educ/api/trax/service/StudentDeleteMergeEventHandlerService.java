package ca.bc.gov.educ.api.trax.service;

import ca.bc.gov.educ.api.trax.messaging.MessagePublisher;
import ca.bc.gov.educ.api.trax.properties.ApplicationProperties;
import ca.bc.gov.educ.api.trax.repository.EventRepository;
import ca.bc.gov.educ.api.trax.rest.RestUtils;
import ca.bc.gov.educ.api.trax.struct.EventType;
import ca.bc.gov.educ.api.trax.struct.Student;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


/**
 * The type Student create merge service.
 */
@Service
@Slf4j
public class StudentDeleteMergeEventHandlerService extends BaseStudentMergeEventHandlerService {


  /**
   * Instantiates a new Student delete merge service.
   *
   * @param eventRepository  the event repository
   * @param restUtils        the rest utils
   * @param chesEmailService the ches email service
   */
  @Autowired
  public StudentDeleteMergeEventHandlerService(final EventRepository eventRepository, final RestUtils restUtils, final CHESEmailService chesEmailService, final ApplicationProperties applicationProperties, final MessagePublisher messagePublisher) {
    super(restUtils, chesEmailService, eventRepository, applicationProperties, messagePublisher);
  }


  @Override
  public String getEventType() {
    return EventType.DELETE_MERGE.toString();
  }

  /**
   * notify only when one of the students is present in GRAD-STUDENT-API.
   * From: pens.coordinator@gov.bc.ca
   * To: student.certification@gov.bc.ca
   * Subject: MERGE DIFFERENCE: 123456789 MERGED TO 456789123 IN PEN, NOT MERGED IN TRAX
   */
  @Override
  protected void processStudentsMergeInfo(final Student student, final Student trueStudent) {
    final String pen = student.getPen();
    final String mergedToPen = trueStudent.getPen();
    final String studentId = student.getStudentID();
    final String mergedToStudentId = trueStudent.getStudentID();
    log.info("PEN from API calls PEN {} True PEN {}, Student IDs: {} and {}", pen, mergedToPen, studentId, mergedToStudentId);
    
    try {
      if (checkEitherStudentExistsInGradStudentApi(studentId, mergedToStudentId)) {
        log.info("Either student is present in GRAD-STUDENT-API, notifying...");
        this.prepareAndSendEmail(pen, mergedToPen);
      } else {
        log.info("Neither student found in GRAD-STUDENT-API");
      }
    } catch (Exception e) {
      log.error("Error checking student existence in GRAD-STUDENT-API for Student IDs: {} and {}", studentId, mergedToStudentId, e);
      throw new RuntimeException("Failed to check students. This should not have happened and will be retried.");
    }
  }

  private boolean checkEitherStudentExistsInGradStudentApi(final String studentId, final String mergedToStudentId) throws Exception {
    log.info("Checking either student {} or {} in GRAD-STUDENT-API in parallel", studentId, mergedToStudentId);
    
    CompletableFuture<Boolean> firstFuture = CompletableFuture.supplyAsync(() -> checkStudentExistsInGradStudentApi(studentId));
    CompletableFuture<Boolean> secondFuture = CompletableFuture.supplyAsync(() -> checkStudentExistsInGradStudentApi(mergedToStudentId));
    
    CompletableFuture.allOf(firstFuture, secondFuture).get();
    
    return firstFuture.get() || secondFuture.get();
  }

  protected void prepareAndSendEmail(final String pen, final String mergedToPen) {
    final String subject = pen.concat(" DEMERGED FROM ").concat(mergedToPen).concat(" IN PEN, NOT DEMERGED IN GRAD");
    this.chesEmailService.sendEmail(null, subject, subject);
  }
}
