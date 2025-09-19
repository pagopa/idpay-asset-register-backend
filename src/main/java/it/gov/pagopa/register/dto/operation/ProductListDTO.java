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
public class ProductListDTO {

  private List<ProductDTO> content;
  private Integer pageNo;
  private Integer pageSize;
  private Long totalElements;
  private Integer totalPages;
}
