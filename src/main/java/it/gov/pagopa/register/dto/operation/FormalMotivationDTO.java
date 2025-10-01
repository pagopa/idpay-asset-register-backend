package it.gov.pagopa.register.dto.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FormalMotivationDTO {
  private String formalMotivation;
  private LocalDateTime updateDate;
}
