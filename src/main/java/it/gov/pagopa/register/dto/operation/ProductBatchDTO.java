package it.gov.pagopa.register.dto.operation;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductBatchDTO {
  private String productFileId;
  private String batchName;
}
