package ca.bc.gov.educ.api.trax.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Grad student.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradStudent {

  private String studentID;
  private String program;
  private String programCompletionDate;
  private String schoolOfRecordId;
  private String studentStatusCode;
  private String graduated;
  private String schoolAtGradId;
  private String studentGrade;
  private String exception;
}
