package ca.bc.gov.educ.api.trax.rest;

import ca.bc.gov.educ.api.trax.properties.ApplicationProperties;
import ca.bc.gov.educ.api.trax.struct.CHESEmail;
import ca.bc.gov.educ.api.trax.struct.Student;
import ca.bc.gov.educ.api.trax.struct.TraxStudent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

/**
 * The type Rest utils.
 */
@Component
@Slf4j
public class RestUtils {
  private final ApplicationProperties props;

  /**
   * The Web client.
   */
  private final WebClient webClient;

  private final WebClient chesWebClient;

  /**
   * Instantiates a new Rest utils.
   *
   * @param props         the props
   * @param webClient     the web client
   * @param chesWebClient the ches web client
   */
  @Autowired
  public RestUtils(final ApplicationProperties props, @Qualifier("webClient") final WebClient webClient, @Qualifier("chesWebClient") final WebClient chesWebClient) {
    this.props = props;
    this.webClient = webClient;
    this.chesWebClient = chesWebClient;
  }

  /**
   * Gets student pen by student id.
   *
   * @param studentID the student id
   * @return the student pen by student id
   */
  @Retryable(value = {Exception.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Mono<Student> getStudentPenByStudentID(final String studentID) {
    log.info("calling student api to get pen number for student ID :: {}", studentID);
    return this.webClient.get().uri(this.props.getStudentApiURL() + "/" + studentID).header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).retrieve().bodyToMono(Student.class);
  }

  public Optional<TraxStudent> getTraxStudentByPen(@NonNull final String pen) {
    log.info("calling trax api to get student  for pen:: {}", pen);
    try{
      return Optional.ofNullable(this.webClient.get()
          .uri(this.props.getTraxApiURL() ,uri -> uri
              .path("/students")
              .queryParam("studNo", pen)
              .build())
          .header(CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE)
          .retrieve()
          .bodyToMono(TraxStudent.class).block());
    }catch (final WebClientResponseException webClientResponseException){
      if(webClientResponseException.getStatusCode() == HttpStatus.NOT_FOUND){
        return Optional.empty();
      }else {
        throw  webClientResponseException;
      }
    }
  }

  /**
   * Send email.
   *
   * @param chesEmail the ches email json object as string
   */
  @Retryable(value = {Exception.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  public void sendEmail(final CHESEmail chesEmail) {
    this.chesWebClient
        .post()
        .uri(this.props.getChesEndpointURL())
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(Mono.just(chesEmail), CHESEmail.class)
        .retrieve().bodyToMono(String.class).subscribeOn(Schedulers.parallel()).doOnError(this::logError).doOnSuccess(this::onSendEmailSuccess).block();
  }

  private void logError(final Throwable throwable) {
    log.error("Error from CHES API call", throwable);
  }

  private void onSendEmailSuccess(final String s) {
    log.info("Email sent success :: {}", s);
  }
}
