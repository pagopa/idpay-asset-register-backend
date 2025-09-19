package it.gov.pagopa.register.dto.operation;

import it.gov.pagopa.register.enums.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateStatusRequestDTO {

  @NotEmpty()
  private List<String> gtinCodes;

  private ProductStatus currentStatus;
  private ProductStatus targetStatus;


  @NotBlank()
  private String motivation;

  @NotBlank()
  private String formalMotivation;
}
