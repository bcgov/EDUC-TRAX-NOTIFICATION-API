package ca.bc.gov.educ.api.trax.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * The type Student merge.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentMerge extends BaseRequest implements Serializable {
  private static final long serialVersionUID = -8133441569621088814L;
  /**
   * The Student merge id.
   */
  String studentMergeID;
  /**
   * The Student id.
   */
  @NotNull(message = "Student ID can not be null.")
  String studentID;
  /**
   * The Merge student id.
   */
  @NotNull(message = "Merge Student ID can not be null.")
  String mergeStudentID;
  /**
   * The Student merge direction code.
   */
  @NotNull(message = "Student Merge Direction Code can not be null.")
  String studentMergeDirectionCode;
  /**
   * The Student merge source code.
   */
  @NotNull(message = "Student Merge Source Code can not be null.")
  String studentMergeSourceCode;
}
