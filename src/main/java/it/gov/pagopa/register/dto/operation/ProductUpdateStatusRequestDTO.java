package it.gov.pagopa.register.dto.operation;

import it.gov.pagopa.register.enums.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductUpdateStatusRequestDTO {
  private List<String> gtinCodes;
  private ProductStatus status;
  @NotBlank
  private String motivation;
}
