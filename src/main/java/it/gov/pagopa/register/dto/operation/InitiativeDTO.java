package it.gov.pagopa.register.dto.operation;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiativeDTO {

  private String initiativeId;
  private String initiativeName;
  private String organizationName;
  private String organizationEmail;
  private InitiativeStatus status;

  private LocalDate startDate;
  private LocalDate endDate;

  private String serviceId;
  private InitiativeGeneralDTO general;
  private InitiativeAdditionalDTO additionalInfo;
  private Boolean enabled;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InitiativeGeneralDTO {
    private LocalDate startDate;
    private LocalDate endDate;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InitiativeAdditionalDTO {
    private String serviceId;
  }
}
