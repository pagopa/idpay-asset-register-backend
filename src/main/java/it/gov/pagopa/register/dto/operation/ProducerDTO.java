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
public class ProducerDTO {

  private String producerId;
  private String producerName;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
