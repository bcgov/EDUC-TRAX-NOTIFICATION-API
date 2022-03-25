package ca.bc.gov.educ.api.trax.schedulers;

import ca.bc.gov.educ.api.trax.model.Event;
import ca.bc.gov.educ.api.trax.repository.EventRepository;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class PurgeOldRecordsSchedulerTest {

  @Autowired
  EventRepository eventRepository;

  @Autowired
  PurgeOldRecordsScheduler purgeOldRecordsScheduler;

  @After
  public void after() {
    this.eventRepository.deleteAll();
  }

  @Test
  public void testPurgeOldRecords_givenOldRecordsPresent_shouldBeDeleted() {
    final var payload = " {\n" +
        "    \"createUser\": \"test\",\n" +
        "    \"updateUser\": \"test\",\n" +
        "    \"legalFirstName\": \"Jack\"\n" +
        "  }";

    final var yesterday = LocalDateTime.now().minusDays(1);

    this.eventRepository.save(this.getEvent(payload, LocalDateTime.now()));

    this.eventRepository.save(this.getEvent(payload, yesterday));

    this.purgeOldRecordsScheduler.setEventRecordStaleInDays(1);
    this.purgeOldRecordsScheduler.purgeOldRecords();

    final var servicesEvents = this.eventRepository.findAll();
    assertThat(servicesEvents).hasSize(1);
  }


  private Event getEvent(final String payload, final LocalDateTime createDateTime) {
    return Event
      .builder()
      .eventPayloadBytes(payload.getBytes())
      .eventStatus("PROCESSED")
      .eventType("CREATE_MERGE")
      .eventOutcome("MERGE_CREATED")
      .createDate(createDateTime)
      .createUser("TRAX_NOTIFICATION_API")
      .updateUser("TRAX_NOTIFICATION_API")
      .updateDate(createDateTime)
      .build();
  }
}
