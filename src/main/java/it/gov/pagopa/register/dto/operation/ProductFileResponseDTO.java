package it.gov.pagopa.register.dto.operation;
import it.gov.pagopa.register.model.operation.ProductFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductFileResponseDTO {
  private List<ProductFileDTO> content;
  private long totalElements;
}
