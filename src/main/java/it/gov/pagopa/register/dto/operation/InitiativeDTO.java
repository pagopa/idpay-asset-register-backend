package it.gov.pagopa.register.dto.operation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InitiativeDTO {

  private String initiativeId;
  private String initiativeName;
  private String organizationId;
  private String organizationName;

  private InitiativeStatus status;

  private LocalDate startDate;
  private LocalDate endDate;

  private String serviceId;
  private Boolean enabled;

  private InitiativeGeneralDTO general;
  private InitiativeAdditionalDTO additionalInfo;

  public LocalDate getStartDate() {
    return startDate != null ? startDate : general != null ? general.getStartDate() : null;
  }

  public LocalDate getEndDate() {
    return endDate != null ? endDate : general != null ? general.getEndDate() : null;
  }

  public String getServiceId() {
    return serviceId != null ? serviceId : additionalInfo != null ? additionalInfo.getServiceId() : null;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class InitiativeGeneralDTO {
    private LocalDate startDate;
    private LocalDate endDate;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class InitiativeAdditionalDTO {
    private String serviceId;
  }
}
