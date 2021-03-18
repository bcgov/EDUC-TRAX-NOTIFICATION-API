package ca.bc.gov.educ.api.trax.service;

import ca.bc.gov.educ.api.trax.constants.EventStatus;
import ca.bc.gov.educ.api.trax.model.Event;
import ca.bc.gov.educ.api.trax.properties.ApplicationProperties;
import ca.bc.gov.educ.api.trax.repository.EventRepository;
import ca.bc.gov.educ.api.trax.rest.RestUtils;
import ca.bc.gov.educ.api.trax.struct.Student;
import ca.bc.gov.educ.api.trax.struct.StudentMerge;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * The type Base student service.
 */
@Slf4j
public abstract class BaseStudentMergeEventHandlerService implements EventHandlerService {
  protected final RestUtils restUtils;
  protected final CHESEmailService chesEmailService;
  protected final EventRepository eventRepository;
  protected final ApplicationProperties applicationProperties;

  protected BaseStudentMergeEventHandlerService(final RestUtils restUtils, final CHESEmailService chesEmailService, final EventRepository eventRepository, ApplicationProperties applicationProperties) {
    this.restUtils = restUtils;
    this.chesEmailService = chesEmailService;
    this.eventRepository = eventRepository;
    this.applicationProperties = applicationProperties;
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
  private void processMergeTO(final StudentMerge studentMerge) {
    if (StringUtils.isNotBlank(applicationProperties.getToEmail())) {
      final String studentID = studentMerge.getStudentID();
      final String mergedToStudentID = studentMerge.getMergeStudentID();
      final Mono<Student> studentMono = this.restUtils.getStudentPenByStudentID(studentID).subscribeOn(Schedulers.parallel());
      final Mono<Student> mergedToStudentMono = this.restUtils.getStudentPenByStudentID(mergedToStudentID).subscribeOn(Schedulers.parallel());
      this.processStudentsMergeInfo(Objects.requireNonNull(Mono.zip(studentMono, mergedToStudentMono).block()));
    }
  }


  /**
   * notify only when one of the students is present in trax.
   * From: pens.coordinator@gov.bc.ca
   * To: student.certification@gov.bc.ca
   * Subject: MERGE DIFFERENCE: 123456789 MERGED TO 456789123 IN PEN, NOT MERGED IN TRAX
   */
  private void processStudentsMergeInfo(final Tuple2<Student, Student> studentTuple) {
    final String pen = studentTuple.getT1().getPen();
    final String mergedToPen = studentTuple.getT2().getPen();
    log.info("PEN from API calls PEN {} True PEN {}", pen, mergedToPen);
    val traxStudentOptional = restUtils.getTraxStudentByPen(pen);
    val traxMergedToStudentOptional = restUtils.getTraxStudentByPen(mergedToPen);
    if (traxStudentOptional.isPresent() || traxMergedToStudentOptional.isPresent()) {
      log.info("either one or both the students are present in trax, notifying...");
      this.prepareAndSendEmail(pen, mergedToPen);
    }
  }

  private boolean mergeToPredicate(final StudentMerge studentMerge) {
    return StringUtils.equals(studentMerge.getStudentMergeDirectionCode(), "TO");
  }

  protected abstract void prepareAndSendEmail(String pen, String mergedToPEN);
}
