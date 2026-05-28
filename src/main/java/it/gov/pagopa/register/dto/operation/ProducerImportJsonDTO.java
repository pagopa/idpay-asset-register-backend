package it.gov.pagopa.register.dto.operation;

import lombok.Data;

@Data
public class ProducerImportJsonDTO {
  private String producerId;
  private String initiativeId;
  private String producerEmail;
  private String producerName;
}
