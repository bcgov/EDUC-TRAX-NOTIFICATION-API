package ca.bc.gov.educ.api.trax.properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;


@ActiveProfiles("test")
@SpringBootTest
@RunWith(SpringRunner.class)
public class ApplicationPropertiesTest {

  @Autowired
  ApplicationProperties props;

  @Test
  public void test() {
    assertThat(this.props.getChesClientID()).isNotNull();
    assertThat(this.props.getChesClientSecret()).isNotNull();
    assertThat(this.props.getChesEndpointURL()).isNotNull();
    assertThat(this.props.getChesTokenURL()).isNotNull();
    assertThat(this.props.getClientID()).isNotNull();
    assertThat(this.props.getClientSecret()).isNotNull();
    assertThat(this.props.getFromEmail()).isNotNull();
    assertThat(this.props.getNatsUrl()).isNotNull();
    assertThat(this.props.getNatsMaxReconnect()).isNotNull();
    assertThat(this.props.getStudentApiURL()).isNotNull();
    assertThat(this.props.getToEmail()).isNotNull();
    assertThat(this.props.getTokenURL()).isNotNull();
    assertThat(ApplicationProperties.API_NAME).isNotNull();
  }
}
