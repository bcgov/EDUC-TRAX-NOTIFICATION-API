package ca.bc.gov.educ.api.trax.service;

import ca.bc.gov.educ.api.trax.constants.EventStatus;
import ca.bc.gov.educ.api.trax.exception.BusinessError;
import ca.bc.gov.educ.api.trax.exception.BusinessException;
import ca.bc.gov.educ.api.trax.model.Event;
import ca.bc.gov.educ.api.trax.repository.EventRepository;
import ca.bc.gov.educ.api.trax.struct.ChoreographedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


/**
 * The type Choreographed event persistence service.
 */
@Service
@Slf4j
public class ChoreographedEventPersistenceService {
  private final EventRepository eventRepository;

  /**
   * Instantiates a new Choreographed event persistence service.
   *
   * @param eventRepository the event repository
   */
  @Autowired
  public ChoreographedEventPersistenceService(final EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  /**
   * Persist event to db event.
   *
   * @param choreographedEvent the choreographed event
   * @return the event
   * @throws BusinessException the business exception
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Event persistEventToDB(final ChoreographedEvent choreographedEvent) throws BusinessException {
    final var eventOptional = this.eventRepository.findByEventId(choreographedEvent.getEventID());
    if (eventOptional.isPresent()) {
      throw new BusinessException(BusinessError.EVENT_ALREADY_PERSISTED, choreographedEvent.getEventID().toString());
    }
    final Event event = Event.builder()
        .eventType(choreographedEvent.getEventType().toString())
        .eventId(choreographedEvent.getEventID())
        .eventOutcome(choreographedEvent.getEventOutcome().toString())
        .eventPayload(choreographedEvent.getEventPayload())
        .eventStatus(EventStatus.DB_COMMITTED.toString())
        .createUser(StringUtils.isBlank(choreographedEvent.getCreateUser()) ? "TRAX-NOTIFICATION-API" : choreographedEvent.getCreateUser())
        .updateUser(StringUtils.isBlank(choreographedEvent.getUpdateUser()) ? "TRAX-NOTIFICATION-API" : choreographedEvent.getUpdateUser())
        .createDate(LocalDateTime.now())
        .updateDate(LocalDateTime.now())
        .build();
    return this.eventRepository.save(event);
  }
}
