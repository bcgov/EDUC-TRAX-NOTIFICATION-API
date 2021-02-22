package ca.bc.gov.educ.api.trax.choreographer;

import ca.bc.gov.educ.api.trax.model.Event;
import ca.bc.gov.educ.api.trax.service.EventHandlerService;
import ca.bc.gov.educ.api.trax.struct.EventType;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * This class is responsible to handle different choreographed events related student by calling different services.
 */
@Component
@Slf4j
public class StudentChoreographer {
  private final Executor singleTaskExecutor = new EnhancedQueueExecutor.Builder()
      .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("task-executor-%d").build())
      .setCorePoolSize(1).setMaximumPoolSize(1).build();
  private final Map<String, EventHandlerService> eventServiceMap;

  /**
   * Instantiates a new Student choreographer.
   *
   * @param eventHandlerServices the event services
   */
  public StudentChoreographer(final List<EventHandlerService> eventHandlerServices) {
    this.eventServiceMap = eventHandlerServices.stream().collect(Collectors.toMap(EventHandlerService::getEventType, Function.identity()));
  }

  /**
   * Handle event.
   *
   * @param event the event
   */
  public void handleEvent(@NonNull final Event event) {
    //only one thread will process all the request.
    this.singleTaskExecutor.execute(() -> {
      try {
        switch (event.getEventType()) {
          case "CREATE_MERGE":
            this.eventServiceMap.get(EventType.CREATE_MERGE.toString()).processEvent(event);
            break;
          case "DELETE_MERGE":
            this.eventServiceMap.get(EventType.DELETE_MERGE.toString()).processEvent(event);
            break;
          default:
            break;
        }
      } catch (final Exception exception) {
        log.error("Exception while processing event :: {}", event, exception);
      }
    });
  }
}
