package it.gov.pagopa.register.dto.operation;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProducerImportJsonDTO {
  private String producerId;
  private String initiativeId;
  private String producerEmail;
  private String producerName;
}
