package it.gov.pagopa.register.dto;

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
  private Long pageNo;
  private Long pageSize;
  private Long totalElements;
  private Long totalPages;
}
