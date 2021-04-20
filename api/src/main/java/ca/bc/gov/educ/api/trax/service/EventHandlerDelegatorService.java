package ca.bc.gov.educ.api.trax.service;

import ca.bc.gov.educ.api.trax.choreographer.StudentChoreographer;
import ca.bc.gov.educ.api.trax.exception.BusinessException;
import ca.bc.gov.educ.api.trax.struct.ChoreographedEvent;
import io.nats.client.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;


/**
 * This class is responsible to process events from Jet Stream.
 */
@Service
@Slf4j
public class EventHandlerDelegatorService {

  private final ChoreographedEventPersistenceService choreographedEventPersistenceService;
  private final StudentChoreographer studentChoreographer;

  /**
   * Instantiates a new Event handler delegator service.
   *
   * @param choreographedEventPersistenceService the choreographed event persistence service
   * @param studentChoreographer                 the student choreographer
   */
  @Autowired
  public EventHandlerDelegatorService(final ChoreographedEventPersistenceService choreographedEventPersistenceService, final StudentChoreographer studentChoreographer) {
    this.choreographedEventPersistenceService = choreographedEventPersistenceService;
    this.studentChoreographer = studentChoreographer;
  }

  /**
   * this method will do the following.
   * 1. Call service to store the event in oracle DB.
   * 2. Acknowledge to  Jet Stream only when the service call is completed. since it uses manual acknowledgement.
   * 3. Hand off the task to update RDB onto a different executor.
   *
   * @param choreographedEvent the choreographed event
   * @param message            the message
   */
  public void handleChoreographyEvent(@NonNull final ChoreographedEvent choreographedEvent, final Message message) {
    try {
      final var persistedEvent = this.choreographedEventPersistenceService.persistEventToDB(choreographedEvent);
      message.ack(); // acknowledge to Jet Stream that api got the message and it is now in DB.
      log.info("acknowledged to Jet Stream...");
      this.studentChoreographer.handleEvent(persistedEvent);
    } catch (final BusinessException businessException) {
      message.ack(); // acknowledge to Jet Stream that api got the message already...
      log.info("acknowledged to  Jet Stream...");
    }
  }
}
