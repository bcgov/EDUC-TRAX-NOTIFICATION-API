package ca.bc.gov.educ.api.trax.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * The type Student.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("java:S1948")
public class Student extends BaseStudent implements Serializable {
  private static final long serialVersionUID = 1L;

  @Override
  public String toString() {
    return super.toString();
  }
}
