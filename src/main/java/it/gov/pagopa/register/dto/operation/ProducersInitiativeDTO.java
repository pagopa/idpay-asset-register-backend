package it.gov.pagopa.register.dto.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProducersInitiativeDTO {

  private String producerId;
  private String producerEmail;
  private String producerName;
  private String initiativeId;
  private String initiativeName;
  private String initiativeStatus;

  private LocalDateTime initiativeStartDate;
  private LocalDateTime initiativeEndDate;

  private String initiativeServiceId;
  private String initiativeOrganizationName;

  private Boolean enabled;
}

