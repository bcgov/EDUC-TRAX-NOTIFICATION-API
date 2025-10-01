package ca.bc.gov.educ.api.trax.service;

import ca.bc.gov.educ.api.trax.constants.EventStatus;
import ca.bc.gov.educ.api.trax.messaging.MessagePublisher;
import ca.bc.gov.educ.api.trax.model.Event;
import ca.bc.gov.educ.api.trax.properties.ApplicationProperties;
import ca.bc.gov.educ.api.trax.repository.EventRepository;
import ca.bc.gov.educ.api.trax.rest.RestUtils;
import ca.bc.gov.educ.api.trax.struct.EventOutcome;
import ca.bc.gov.educ.api.trax.struct.EventType;
import ca.bc.gov.educ.api.trax.struct.Student;
import ca.bc.gov.educ.api.trax.struct.StudentMerge;
import ca.bc.gov.educ.api.trax.struct.GradStudent;
import ca.bc.gov.educ.api.trax.util.JsonUtil;
import ca.bc.gov.educ.api.trax.exception.NotificationApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The type Base student service.
 */
@Slf4j
public abstract class BaseStudentMergeEventHandlerService implements EventHandlerService {
  protected final RestUtils restUtils;
  protected final CHESEmailService chesEmailService;
  protected final EventRepository eventRepository;
  protected final ApplicationProperties applicationProperties;
  protected final MessagePublisher messagePublisher;
  /**
   * The Object mapper.
   */
  private final ObjectMapper objectMapper = new ObjectMapper();

  protected BaseStudentMergeEventHandlerService(final RestUtils restUtils, final CHESEmailService chesEmailService, final EventRepository eventRepository, final ApplicationProperties applicationProperties, final MessagePublisher messagePublisher) {
    this.restUtils = restUtils;
    this.chesEmailService = chesEmailService;
    this.eventRepository = eventRepository;
    this.applicationProperties = applicationProperties;
    this.messagePublisher = messagePublisher;
  }

  protected void markRecordAsProcessed(final Event event) {
    event.setEventStatus(EventStatus.PROCESSED.toString());
    event.setUpdateDate(LocalDateTime.now());
    this.eventRepository.save(event);
    log.info("event processed {}", event.getEventId());
  }


  @Override
  public void processEvent(final Event event) throws JsonProcessingException {
    log.info("processing event {}", event);
    final List<StudentMerge> studentMerges = new ObjectMapper().readValue(event.getEventPayload(), new TypeReference<>() {
    });
    studentMerges.stream().filter(this::mergeToPredicate).findFirst().ifPresent(this::processMergeTO);
    this.eventRepository.findByEventId(event.getEventId()).ifPresent(this::markRecordAsProcessed);
  }


  /**
   * if the toEmail is blank mark the record as processed , as system is not going to notify.
   */
  @SneakyThrows(JsonProcessingException.class)
  private void processMergeTO(final StudentMerge studentMerge) {
    if (StringUtils.isNotBlank(this.applicationProperties.getToEmail())) {
      final String studentID = studentMerge.getStudentID();
      final String mergedToStudentID = studentMerge.getMergeStudentID();
      final List<String> studentIDs = new ArrayList<>();
      studentIDs.add(studentID);
      studentIDs.add(mergedToStudentID);
      log.info("called STUDENT_API to get students :: {}", studentIDs);
      final var event = ca.bc.gov.educ.api.trax.struct.Event.builder().sagaId(UUID.randomUUID()).eventType(EventType.GET_STUDENTS).eventPayload(JsonUtil.getJsonStringFromObject(studentIDs)).build();
      var responseEvent = new ca.bc.gov.educ.api.trax.struct.Event();
      var i = 0;
      while (i < 3) {
        try {
          responseEvent = JsonUtil.getJsonObjectFromByteArray(ca.bc.gov.educ.api.trax.struct.Event.class,
            this.messagePublisher.requestMessage("STUDENT_API_TOPIC", JsonUtil.getJsonBytesFromObject(event)).get(5, TimeUnit.SECONDS).getData());
          i = 3; // break out of loop
        } catch (final IOException | ExecutionException | TimeoutException e) {
          log.error("exception while getting student data", e);
          i++;
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          log.error("exception while getting student data", e);
          i++;
        }
      }
      log.info("got response from STUDENT_API  :: {}", responseEvent);
      if (responseEvent.getEventOutcome() == EventOutcome.STUDENTS_NOT_FOUND) {
        log.error("Students not found or student size mismatch for student IDs:: {}, this should not have happened", studentIDs);
        return;
      }
      final List<Student> students = this.objectMapper.readValue(responseEvent.getEventPayload(), new TypeReference<>() {
      });
      final Map<String, Student> studentMap = students.stream().collect(Collectors.toConcurrentMap(Student::getStudentID, Function.identity()));
      val student = studentMap.get(studentID);
      val trueStudent = studentMap.get(mergedToStudentID);
      this.processStudentsMergeInfo(student, trueStudent);
    }
  }


  /**
   * notify only when one of the students is present in trax.
   * From: pens.coordinator@gov.bc.ca
   * To: student.certification@gov.bc.ca
   * Subject: MERGE DIFFERENCE: 123456789 MERGED TO 456789123 IN PEN, NOT MERGED IN TRAX
   */
  protected abstract void processStudentsMergeInfo(final Student student, final Student trueStudent);

  /**
   * Check if a single student exists in GRAD-STUDENT-API using GET_STUDENT event
   */
  protected boolean checkStudentExistsInGradStudentApi(final String studentId) {
    try {
      final var event = ca.bc.gov.educ.api.trax.struct.Event.builder()
          .sagaId(java.util.UUID.randomUUID())
          .eventType(EventType.GET_STUDENT)
          .eventPayload(studentId)
          .build();
      
      log.info("Sending GET_STUDENT event to GRAD_STUDENT_API for Student ID: {}", studentId);

      String responseJson = new String(this.messagePublisher.requestMessage("GRAD_STUDENT_API_FETCH_GRAD_STUDENT_TOPIC", JsonUtil.getJsonBytesFromObject(event))
          .get(5, java.util.concurrent.TimeUnit.SECONDS).getData());
      
      log.debug("Got response from GRAD_STUDENT_API for Student ID {}: {}", studentId, responseJson);

      GradStudent gradStudent = this.objectMapper.readValue(responseJson, GradStudent.class);
      
      if (gradStudent.getException() != null && !gradStudent.getException().isEmpty()) {
        if ("not found".equalsIgnoreCase(gradStudent.getException())) {
          log.info("Student {} not found in GRAD-STUDENT-API", studentId);
          return false;
        } else {
          throw new NotificationApiException("GRAD-STUDENT-API error for student " + studentId + ": " + gradStudent.getException());
        }
      }
      log.info("Student {} found in GRAD-STUDENT-API", studentId);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted while checking student {}: {}", studentId, e.getMessage());
      throw new NotificationApiException("Interrupted while checking student " + studentId, e);
    } catch (Exception e) {
      log.error("Error checking student {}: {}", studentId, e.getMessage());
      throw new NotificationApiException("Failed to check student " + studentId, e);
    }
  }

  private boolean mergeToPredicate(final StudentMerge studentMerge) {
    return StringUtils.equals(studentMerge.getStudentMergeDirectionCode(), "TO");
  }

}
