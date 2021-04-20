package ca.bc.gov.educ.api.trax.messaging.jetstream;

import ca.bc.gov.educ.api.trax.constants.Topics;
import ca.bc.gov.educ.api.trax.properties.ApplicationProperties;
import ca.bc.gov.educ.api.trax.service.EventHandlerDelegatorService;
import ca.bc.gov.educ.api.trax.struct.ChoreographedEvent;
import ca.bc.gov.educ.api.trax.struct.EventType;
import ca.bc.gov.educ.api.trax.util.JsonUtil;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;


/**
 * The type Subscriber.
 */
@Component
@Slf4j
public class Subscriber {

  private final EventHandlerDelegatorService eventHandlerDelegatorService;
  private final Map<String, List<String>> streamTopicsMap = new HashMap<>(); // one stream can have multiple topics.
  private final Connection natsConnection;

  /**
   * Instantiates a new Subscriber.
   *
   * @param natsConnection               the nats connection
   * @param eventHandlerDelegatorService the event handler delegator service
   */
  @Autowired
  public Subscriber(final Connection natsConnection, final EventHandlerDelegatorService eventHandlerDelegatorService) {
    this.eventHandlerDelegatorService = eventHandlerDelegatorService;
    this.natsConnection = natsConnection;
    this.initializeStreamTopicMap();
  }

  /**
   * this is the source of truth for all the topics this api subscribes to.
   */
  private void initializeStreamTopicMap() {
    List<String> penServicesEventsTopics = new ArrayList<>();
    penServicesEventsTopics.add("PEN_SERVICES_EVENTS_TOPIC");
    this.streamTopicsMap.put("PEN_SERVICES_EVENTS", penServicesEventsTopics);
  }

  @PostConstruct
  public void subscribe() throws IOException, JetStreamApiException {
    val qName = ApplicationProperties.API_NAME.concat("-QUEUE");
    val autoAck = false;
    for (val entry : streamTopicsMap.entrySet()) {
      for(val topic: entry.getValue()){
        PushSubscribeOptions options = PushSubscribeOptions.builder().stream(entry.getKey())
            .durable(ApplicationProperties.API_NAME.concat("-DURABLE"))
            .configuration(ConsumerConfiguration.builder().deliverPolicy(DeliverPolicy.New).build()).build();
        this.natsConnection.jetStream().subscribe(topic, qName, this.natsConnection.createDispatcher(), this::onMessage,
            autoAck, options);
      }
    }
  }

  /**
   * This method will process the event message pushed into different topics of different APIS.
   * All APIs publish ChoreographedEvent
   *
   * @param message the string representation of {@link ChoreographedEvent} if it not type of event then it will throw exception and will be ignored.
   */
  public void onMessage(final Message message) {
    if (message != null) {
      try {
        final String eventString = new String(message.getData());
        final ChoreographedEvent event = JsonUtil.getJsonObjectFromString(ChoreographedEvent.class, eventString);
        if (event.getEventPayload() == null) {
          message.ack();
          log.warn("payload is null, ignoring event :: {}", event);
          return;
        }
        if (event.getEventType().equals(EventType.CREATE_MERGE) || event.getEventType().equals(EventType.DELETE_MERGE)) {
          this.eventHandlerDelegatorService.handleChoreographyEvent(event, message);
        } else {
          message.ack();
          log.warn("API not interested in other events, ignoring event :: {}", event);
        }

      } catch (final Exception ex) {
        log.error("Exception ", ex);
      }
    }
  }

}
