package it.gov.pagopa.register.dto.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProducersResponseDTO {

  private List<ProducerDTO> content;
  private long pageNo;
  private long pageSize;
  private long totalElements;
  private long totalPages;
}
