package it.gov.pagopa.register.dto.operation;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
@Builder
public class EmailProductDTO {
  @Id
  private String id;
  private List<String> productNames;

}
