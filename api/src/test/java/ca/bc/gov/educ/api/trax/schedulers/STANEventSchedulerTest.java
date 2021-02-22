package ca.bc.gov.educ.api.trax.schedulers;

import ca.bc.gov.educ.api.trax.constants.EventStatus;
import ca.bc.gov.educ.api.trax.model.Event;
import ca.bc.gov.educ.api.trax.repository.EventRepository;
import ca.bc.gov.educ.api.trax.rest.RestUtils;
import ca.bc.gov.educ.api.trax.service.CHESEmailService;
import ca.bc.gov.educ.api.trax.struct.*;
import ca.bc.gov.educ.api.trax.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ca.bc.gov.educ.api.trax.constants.EventStatus.DB_COMMITTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class STANEventSchedulerTest {
  @Autowired
  RestUtils restUtils;

  @Autowired
  CHESEmailService chesEmailService;


  @Autowired
  STANEventScheduler scheduler;

  @Autowired
  EventRepository eventRepository;
  @Captor
  ArgumentCaptor<CHESEmail> chesEmailArgumentCaptorDeleteMerge;

  @Before
  public void setUp() {
    Mockito.validateMockitoUsage();
    this.eventRepository.deleteAll();
  }

  @After
  public void tearDown() {
    Mockito.validateMockitoUsage();
    this.eventRepository.deleteAll();
  }


  @Test
  public void testFindAndPublishStudentEventsToSTAN_givenRecordInDB_shouldRetrySendingEmail() throws JsonProcessingException, InterruptedException {
    val eventID = UUID.randomUUID();
    this.eventRepository.save(this.createDeleteMergeChoreographyEvent(eventID));
    when(this.restUtils.getStudentPenByStudentID(any())).thenReturn(Mono.just(this.createMockStudent()));
    doNothing().when(this.restUtils).sendEmail(this.chesEmailArgumentCaptorDeleteMerge.capture());
    this.scheduler.findAndPublishStudentEventsToSTAN();
    this.waitForAsyncToFinish(eventID);
    val email = this.chesEmailArgumentCaptorDeleteMerge.getValue();
    assertThat(email).isNotNull();
    assertThat(email.getSubject()).isNotNull();
    assertThat(email.getSubject()).contains("DEMERGED FROM");
  }

  private Event createDeleteMergeChoreographyEvent(final UUID eventID) throws JsonProcessingException {
    final Event event = new Event();
    event.setEventType(EventType.DELETE_MERGE.toString());
    event.setEventOutcome(EventOutcome.MERGE_CREATED.toString());
    event.setEventPayload(JsonUtil.getJsonStringFromObject(this.createStudentMergePayload()));
    event.setEventId(eventID);
    event.setEventStatus(DB_COMMITTED.toString());
    event.setCreateDate(LocalDateTime.now().minusMinutes(7));
    event.setUpdateDate(LocalDateTime.now().minusMinutes(7));
    event.setCreateUser("TEST");
    event.setUpdateUser("TEST");
    return event;
  }

  private List<StudentMerge> createStudentMergePayload() {
    final List<StudentMerge> studentMerges = new ArrayList<>();
    final StudentMerge merge = new StudentMerge();
    merge.setStudentID("7f000101-7151-1d84-8171-5187006c0001");
    merge.setMergeStudentID("7f000101-7151-1d84-8171-5187006c0003");
    merge.setStudentMergeDirectionCode("TO");
    merge.setStudentMergeSourceCode("MI");
    studentMerges.add(merge);
    return studentMerges;
  }

  private Student createMockStudent() {
    final Student student = new Student();
    student.setPen("123456789");
    return student;
  }

  private void waitForAsyncToFinish(final UUID eventID) throws InterruptedException {
    int i = 0;
    while (true) {
      if (i >= 50) {
        break; // break out after trying for 5 seconds.
      }
      val event = this.eventRepository.findByEventId(eventID);
      if (event.isPresent()) {
        if (EventStatus.PROCESSED.toString().equalsIgnoreCase(event.get().getEventStatus())) {
          break;
        }
      }
      TimeUnit.MILLISECONDS.sleep(50);
      i++;
    }
  }
}
