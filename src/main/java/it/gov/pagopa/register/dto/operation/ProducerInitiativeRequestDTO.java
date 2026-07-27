package it.gov.pagopa.register.dto.operation;

import lombok.Data;

@Data
public class ProducerInitiativeRequestDTO {
  private String initiativeId;
  private String producerId;
  private String producerName;
  private String producerEmail;
}
