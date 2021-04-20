package ca.bc.gov.educ.api.trax.schedulers;

import ca.bc.gov.educ.api.trax.choreographer.StudentChoreographer;
import ca.bc.gov.educ.api.trax.constants.EventStatus;
import ca.bc.gov.educ.api.trax.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Collectors;


/**
 * This class is responsible to check the TRAX_NOTIFICATION_EVENT table periodically and process them if they are not yet processed.
 * this is a very edge case scenario which will occur.
 */
@Component
@Slf4j
public class JetStreamEventScheduler {

  /**
   * The Event repository.
   */
  private final EventRepository eventRepository;

  private final StudentChoreographer studentChoreographer;


  /**
   * Instantiates a new Stan event scheduler.
   *
   * @param eventRepository      the event repository
   * @param studentChoreographer the student choreographer
   */
  public JetStreamEventScheduler(final EventRepository eventRepository, final StudentChoreographer studentChoreographer) {
    this.eventRepository = eventRepository;
    this.studentChoreographer = studentChoreographer;
  }

  /**
   * Find and publish student events to jet stream.
   */
  @Scheduled(cron = "${cron.scheduled.process.events.stan}") // every 5 minutes
  @SchedulerLock(name = "PROCESS_CHOREOGRAPHED_EVENTS_FROM_JET_STREAM", lockAtLeastFor = "${cron.scheduled.process.events.stan.lockAtLeastFor}", lockAtMostFor = "${cron.scheduled.process.events" +
      ".stan.lockAtMostFor}")
  public void findAndProcessEvents() {
    LockAssert.assertLocked();
    this.eventRepository.findAllByEventStatus(EventStatus.DB_COMMITTED.toString())
        .stream()
        .filter(el -> el.getUpdateDate().isBefore(LocalDateTime.now().minusMinutes(5)))
        .collect(Collectors.toList())
        .forEach(this.studentChoreographer::handleEvent);
  }
}
