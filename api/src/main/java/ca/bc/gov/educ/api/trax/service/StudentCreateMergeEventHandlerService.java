package ca.bc.gov.educ.api.trax.service;

import ca.bc.gov.educ.api.trax.messaging.MessagePublisher;
import ca.bc.gov.educ.api.trax.properties.ApplicationProperties;
import ca.bc.gov.educ.api.trax.repository.EventRepository;
import ca.bc.gov.educ.api.trax.rest.RestUtils;
import ca.bc.gov.educ.api.trax.struct.EventType;
import ca.bc.gov.educ.api.trax.struct.Student;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


/**
 * The type Student create merge service.
 */
@Service
@Slf4j
public class StudentCreateMergeEventHandlerService extends BaseStudentMergeEventHandlerService {


  /**
   * Instantiates a new Student create merge service.
   *
   * @param eventRepository  the event repository
   * @param restUtils        the rest utils
   * @param chesEmailService the ches email service
   */
  @Autowired
  public StudentCreateMergeEventHandlerService(final EventRepository eventRepository, final RestUtils restUtils, final CHESEmailService chesEmailService, final ApplicationProperties applicationProperties, final MessagePublisher messagePublisher) {
    super(restUtils, chesEmailService, eventRepository, applicationProperties, messagePublisher);
  }


  @Override
  public String getEventType() {
    return EventType.CREATE_MERGE.toString();
  }

  /**
   * notify only when one of the students is present in trax.
   * From: pens.coordinator@gov.bc.ca
   * To: student.certification@gov.bc.ca
   * Subject: MERGE DIFFERENCE: 123456789 MERGED TO 456789123 IN PEN, NOT MERGED IN TRAX
   */
  @Override
  protected void processStudentsMergeInfo(final Student student, final Student trueStudent) {
    final String pen = student.getPen();
    final String mergedToPen = trueStudent.getPen();
    log.info("PEN from API calls PEN {} True PEN {}", pen, mergedToPen);
    val traxStudentOptional = this.restUtils.getTraxStudentByPen(pen);
    val traxMergedToStudentOptional = this.restUtils.getTraxStudentByPen(mergedToPen);
    val result = Mono.zip(traxStudentOptional, traxMergedToStudentOptional).block();
    if (result != null && (result.getT1().isPresent() && result.getT2().isPresent())) {
      log.info("Both the students are present in TRAX, notifying...");
      this.prepareAndSendEmail(pen, mergedToPen);
    }
  }

  protected void prepareAndSendEmail(final String pen, final String mergedToPen) {
    final String subject = "MERGE DIFFERENCE: ".concat(pen).concat(" MERGED TO ").concat(mergedToPen).concat(" IN PEN, NOT MERGED IN TRAX");
    this.chesEmailService.sendEmail(null, subject, subject);
  }
}
