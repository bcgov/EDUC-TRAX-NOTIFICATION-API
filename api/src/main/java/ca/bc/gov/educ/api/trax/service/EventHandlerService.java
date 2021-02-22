package ca.bc.gov.educ.api.trax.service;

import ca.bc.gov.educ.api.trax.model.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.transaction.annotation.Transactional;

/**
 * The interface Event service.
 */
public interface EventHandlerService {

  /**
   * Process event.
   *
   * @param event the event
   * @throws JsonProcessingException the json processing exception
   */
  @Transactional
  void processEvent(Event event) throws JsonProcessingException;

  /**
   * Gets event type.
   *
   * @return the event type
   */
  String getEventType();
}
