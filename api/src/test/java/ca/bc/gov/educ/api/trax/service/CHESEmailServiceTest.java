package ca.bc.gov.educ.api.trax.service;

import ca.bc.gov.educ.api.trax.properties.ApplicationProperties;
import ca.bc.gov.educ.api.trax.rest.RestUtils;
import ca.bc.gov.educ.api.trax.struct.CHESEmail;
import lombok.val;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.validateMockitoUsage;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class CHESEmailServiceTest {

  @Autowired
  RestUtils restUtils;

  @Autowired
  CHESEmailService chesEmailService;
  @Autowired
  ApplicationProperties props;
  @Captor
  ArgumentCaptor<CHESEmail> chesEmailArgumentCaptor;

  @Test
  public void sendEmail_givenValidPayload_shouldSendEmail() {
    doNothing().when(this.restUtils).sendEmail(this.chesEmailArgumentCaptor.capture());
    this.chesEmailService.sendEmail(null, "hello", "hello");
    val chesEmail = this.chesEmailArgumentCaptor.getValue();
    assertThat(chesEmail).isNotNull();
    assertThat(chesEmail.getTo()).isNotEmpty();
    assertThat(chesEmail.getTo().get(0)).isEqualTo(this.props.getToEmail());
  }

  @After
  public void afterEach() {
    validateMockitoUsage();
  }
}
