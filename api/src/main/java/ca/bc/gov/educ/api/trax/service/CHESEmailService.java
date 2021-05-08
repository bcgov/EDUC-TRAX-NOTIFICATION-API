package ca.bc.gov.educ.api.trax.service;

import ca.bc.gov.educ.api.trax.properties.ApplicationProperties;
import ca.bc.gov.educ.api.trax.rest.RestUtils;
import ca.bc.gov.educ.api.trax.struct.CHESEmail;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The type Ches email service.
 */
@Service
@Slf4j
public class CHESEmailService {
  private final RestUtils restUtils;
  private final ApplicationProperties props;

  /**
   * Instantiates a new Ches email service.
   *
   * @param restUtils the rest utils
   * @param props     the props
   */
  @Autowired
  public CHESEmailService(final RestUtils restUtils, final ApplicationProperties props) {
    this.restUtils = restUtils;
    this.props = props;
  }

  /**
   * Send toEmail.
   *  @param toEmail the toEmail
   * @param body    the body
   * @param subject the subject
   */
  public void sendEmail(final String toEmail, final String body, final String subject) {
    val chesEmail = this.getCHESEmailObject(toEmail, body, subject);
    this.restUtils.sendEmail(chesEmail);
  }

  /**
   * Gets ches email json object as string.
   *
   * @param emailAddress the email address
   * @param body         the body
   * @param subject      the subject
   * @return the ches email json object as string
   */
  public CHESEmail getCHESEmailObject(final String emailAddress, final String body, final String subject) {
    val chesEmail = new CHESEmail();
    chesEmail.setBody(body);
    chesEmail.setBodyType("text");
    chesEmail.setDelayTS(0);
    chesEmail.setEncoding("utf-8");
    chesEmail.setFrom(this.props.getFromEmail());
    chesEmail.setPriority("normal");
    chesEmail.setSubject(subject);
    chesEmail.setTag("tag");
    chesEmail.getTo().add(emailAddress == null ? this.props.getToEmail() : emailAddress);
    return chesEmail;
  }

}
