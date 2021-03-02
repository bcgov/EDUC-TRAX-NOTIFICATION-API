package ca.bc.gov.educ.api.trax.service;

import ca.bc.gov.educ.api.trax.repository.EventRepository;
import ca.bc.gov.educ.api.trax.rest.RestUtils;
import ca.bc.gov.educ.api.trax.struct.EventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * The type Student create merge service.
 */
@Service
@Slf4j
public class StudentDeleteMergeEventHandlerService extends BaseStudentMergeEventHandlerService {


  /**
   * Instantiates a new Student create merge service.
   *
   * @param eventRepository  the event repository
   * @param restUtils        the rest utils
   * @param chesEmailService the ches email service
   */
  @Autowired
  public StudentDeleteMergeEventHandlerService(final EventRepository eventRepository, final RestUtils restUtils, final CHESEmailService chesEmailService) {
    super(restUtils, chesEmailService, eventRepository);
  }


  @Override
  public String getEventType() {
    return EventType.DELETE_MERGE.toString();
  }

  @Override
  protected void prepareAndSendEmail(final String pen, final String mergedToPen) {
    final String subject = pen.concat(" DEMERGED FROM ").concat(mergedToPen).concat(" IN PEN, NOT DEMERGED IN TRAX");
    this.chesEmailService.sendEmail(null, subject, subject);
  }
}