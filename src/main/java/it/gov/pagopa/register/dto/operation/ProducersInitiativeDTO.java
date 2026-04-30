package it.gov.pagopa.register.dto.operation;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProducersInitiativeDTO {

  private String producerId;
  private String initiativeId;
  private String initiativeName;
  private String initiativeStatus;

  private LocalDate initiativeStartDate;
  private LocalDate initiativeEndDate;

  private String initiativeServiceId;
  private String initiativeOrganizationName;

  private Boolean enabled;
}

