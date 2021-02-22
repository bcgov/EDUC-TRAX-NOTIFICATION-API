package ca.bc.gov.educ.api.trax.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Ches email.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CHESEmail {

  private String bodyType;
  private String body;
  private Integer delayTS;
  private String encoding;
  private String from;
  private String subject;
  private String priority;
  private List<String> to;
  private String tag;

  /**
   * Gets to.
   *
   * @return the to
   */
  public List<String> getTo() {
    if (this.to == null) {
      this.to = new ArrayList<>();
    }
    return this.to;
  }
}
