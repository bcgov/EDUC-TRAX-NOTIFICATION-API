package ca.bc.gov.educ.api.trax.service;

import ca.bc.gov.educ.api.trax.constants.EventStatus;
import ca.bc.gov.educ.api.trax.repository.EventRepository;
import ca.bc.gov.educ.api.trax.rest.RestUtils;
import ca.bc.gov.educ.api.trax.struct.*;
import ca.bc.gov.educ.api.trax.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.nats.streaming.Message;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class EventHandlerDelegatorServiceTest {

  @Autowired
  RestUtils restUtils;

  @Autowired
  CHESEmailService chesEmailService;


  @Autowired
  EventHandlerDelegatorService eventHandlerDelegatorService;

  @Autowired
  EventRepository eventRepository;

  @Captor
  ArgumentCaptor<CHESEmail> chesEmailArgumentCaptorCreateMerge;
  @Captor
  ArgumentCaptor<CHESEmail> chesEmailArgumentCaptorDeleteMerge;

  @After
  public void afterEach() {
    this.eventRepository.deleteAll();
  }

  @Before
  public void beforeEach() {
    this.eventRepository.deleteAll();
  }

  @Test
  public void handleChoreographyEvent_givenCREATE_MERGE_EVENTAndValidPayload_shouldSendEmail() throws IOException, InterruptedException {
    val eventID = UUID.randomUUID();
    val message = Mockito.mock(Message.class);
    doNothing().when(message).ack();
    when(message.getData()).thenReturn(JsonUtil.getJsonBytesFromObject(this.createChoreographyEvent(eventID)));
    when(this.restUtils.getStudentPenByStudentID(any())).thenReturn(Mono.just(this.createMockStudent()));
    doNothing().when(this.restUtils).sendEmail(this.chesEmailArgumentCaptorCreateMerge.capture());
    this.eventHandlerDelegatorService.handleChoreographyEvent(this.createChoreographyEvent(eventID), message);
    val result = this.eventRepository.findByEventId(eventID);
    assertThat(result).isNotEmpty();
    this.waitForAsyncToFinish(eventID);
    val email = this.chesEmailArgumentCaptorCreateMerge.getValue();
    assertThat(email).isNotNull();
    assertThat(email.getSubject()).isNotNull();
    assertThat(email.getSubject()).contains("MERGE DIFFERENCE:");
    val eventOptional = this.eventRepository.findByEventId(eventID);
    assertThat(eventOptional).isPresent();
    assertThat(eventOptional.get().getEventStatus()).isEqualTo(EventStatus.PROCESSED.toString());
    this.eventHandlerDelegatorService.handleChoreographyEvent(this.createChoreographyEvent(eventID), message);
    val events = this.eventRepository.findAll();
    assertThat(events).isNotEmpty().hasSize(1);// it means the seconds message was discarded.
    verify(message, atLeast(2)).ack();
  }

  @Test
  public void handleChoreographyEvent_givenDELETE_MERGE_EVENTAndValidPayload_shouldSendEmail() throws IOException, InterruptedException {
    Mockito.reset(this.restUtils);
    val eventID = UUID.randomUUID();
    val message = Mockito.mock(Message.class);
    final int numOfInvocation = mockingDetails(message).getInvocations().size();
    doNothing().when(message).ack();
    when(message.getData()).thenReturn(JsonUtil.getJsonBytesFromObject(this.createDeleteMergeChoreographyEvent(eventID)));
    when(this.restUtils.getStudentPenByStudentID(any())).thenReturn(Mono.just(this.createMockStudent()));
    doNothing().when(this.restUtils).sendEmail(this.chesEmailArgumentCaptorDeleteMerge.capture());
    this.eventHandlerDelegatorService.handleChoreographyEvent(this.createDeleteMergeChoreographyEvent(eventID), message);
    val result = this.eventRepository.findByEventId(eventID);
    assertThat(result).isNotEmpty();
    this.waitForAsyncToFinish(eventID);
    val email = this.chesEmailArgumentCaptorDeleteMerge.getValue();
    assertThat(email).isNotNull();
    assertThat(email.getSubject()).isNotNull();
    assertThat(email.getSubject()).contains("DEMERGED FROM");
    val eventOptional = this.eventRepository.findByEventId(eventID);
    assertThat(eventOptional).isPresent();
    assertThat(eventOptional.get().getEventStatus()).isEqualTo(EventStatus.PROCESSED.toString());
    verify(message, atLeast(numOfInvocation + 1)).ack();
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

  private Student createMockStudent() {
    final Student student = new Student();
    student.setPen("123456789");
    return student;
  }

  private ChoreographedEvent createChoreographyEvent(final UUID eventID) throws JsonProcessingException {
    final ChoreographedEvent choreographedEvent = new ChoreographedEvent();
    choreographedEvent.setEventType(EventType.CREATE_MERGE);
    choreographedEvent.setEventOutcome(EventOutcome.MERGE_CREATED);
    choreographedEvent.setEventPayload(JsonUtil.getJsonStringFromObject(this.createStudentMergePayload()));
    choreographedEvent.setEventID(eventID);
    choreographedEvent.setCreateUser("TEST");
    choreographedEvent.setUpdateUser("TEST");
    return choreographedEvent;
  }

  private ChoreographedEvent createDeleteMergeChoreographyEvent(final UUID eventID) throws JsonProcessingException {
    final ChoreographedEvent choreographedEvent = new ChoreographedEvent();
    choreographedEvent.setEventType(EventType.DELETE_MERGE);
    choreographedEvent.setEventOutcome(EventOutcome.MERGE_CREATED);
    choreographedEvent.setEventPayload(JsonUtil.getJsonStringFromObject(this.createStudentMergePayload()));
    choreographedEvent.setEventID(eventID);
    choreographedEvent.setCreateUser("TEST");
    choreographedEvent.setUpdateUser("TEST");
    return choreographedEvent;
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
}
