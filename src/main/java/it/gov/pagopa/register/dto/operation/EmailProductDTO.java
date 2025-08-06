package it.gov.pagopa.register.dto.operation;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EmailProductDTO {

  private String id;
  private List<String> productNames;

}
