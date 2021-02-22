package ca.bc.gov.educ.api.trax.constants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
public class TopicsTest {

  @Test
  public void test() {
    assertThat(Topics.PEN_MATCH_EVENTS_TOPIC).isNotNull();
    assertThat(Topics.PEN_SERVICES_EVENTS_TOPIC).isNotNull();
    assertThat(Topics.STUDENT_EVENTS_TOPIC).isNotNull();
  }
}
