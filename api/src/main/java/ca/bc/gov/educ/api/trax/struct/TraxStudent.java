package ca.bc.gov.educ.api.trax.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * The type Student.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraxStudent implements Serializable {
  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  @Size(max = 10)
  @NotNull(message = "studNo can not be null.")
  private String studNo;

  @Size(max = 1)
  private String archiveFlag;

  @Size(max = 25)
  private String studSurname;

  @Size(max = 25)
  private String studGiven;

  @Size(max = 25)
  private String studMiddle;

  @Size(max = 40)
  private String address1;

  @Size(max = 40)
  private String address2;

  @Size(max = 30)
  private String city;

  @Size(max = 2)
  private String provCode;

  @Size(max = 2)
  private String cntryCode;

  @Size(max = 7)
  private String postal;

  @Size(max = 8)
  private String studBirth;

  @Size(max = 1)
  private String studSex;

  @Size(max = 1)
  private String studCitiz;

  @Size(max = 2)
  private String studGrade;

  @Size(max = 8)
  private String mincode;

  @Size(max = 12)
  private String studLocalId;

  @Size(max = 10)
  private String studTrueNo;

  @Size(max = 9)
  private String studSin;

  @Size(max = 4)
  private String prgmCode;

  @Size(max = 4)
  private String prgmCode2;

  @Size(max = 4)
  private String prgmCode3;

  @Size(max = 1)
  private String studPsiPermit;

  @Size(max = 1)
  private String studRsrchPermit;

  @Size(max = 1)
  private String studStatus;

  @Size(max = 1)
  private String studConsedFlag;

  @Size(max = 4)
  private String yrEnter11;

  private Long gradDate;

  @Size(max = 1)
  private String dogwoodFlag;

  @Size(max = 1)
  private String honourFlag;

  @Size(max = 8)
  private String mincodeGrad;

  @Size(max = 1)
  private String frenchDogwood;

  @Size(max = 4)
  private String prgmCode4;

  @Size(max = 4)
  private String prgmCode5;

  private Long sccDate;

  @Size(max = 4)
  private String gradReqtYear;

  private Long slpDate;

  @Size(max = 10)
  private String mergedFromPen;

  @Size(max = 4)
  private String gradReqtYearAtGrad;

  @Size(max = 2)
  private String studGradeAtGrad;

  private Long xcriptActvDate;

  @Size(max = 1)
  private String allowedAdult;

  @Size(max = 6)
  private String ssaNominationDate;

  @Size(max = 4)
  private String adjTestYear;

  @Size(max = 2)
  private String graduatedAdult;

  @Size(max = 10)
  private String supplierNo;

  @Size(max = 3)
  private String siteNo;

  @Size(max = 40)
  private String emailAddress;

  @Size(max = 2)
  private String englishCert;

  @Size(max = 2)
  private String frenchCert;

  private Long englishCertDate;

  private Long frenchCertDate;
}
