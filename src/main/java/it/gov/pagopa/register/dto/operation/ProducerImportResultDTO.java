package it.gov.pagopa.register.dto.operation;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProducerImportResultDTO {
  private String status;
  private int totalRecords;
  private int importedRecords;
  private int failedRecords;
  private String message;
}
