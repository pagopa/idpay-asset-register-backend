package it.gov.pagopa.register.model.operation;

import it.gov.pagopa.register.enums.ProductStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StatusChangeEvent {
  private String username;
  private String role;
  private String motivation;
  private LocalDateTime updateDate;
  private ProductStatus currentStatus;
  private ProductStatus targetStatus;
}
