package it.gov.pagopa.register.dto.operation;
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
  private long pageNo;
  private long pageSize;
  private long totalElements;
  private long totalPages;
}
