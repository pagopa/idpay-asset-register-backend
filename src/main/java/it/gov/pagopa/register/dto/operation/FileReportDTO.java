package it.gov.pagopa.register.dto.operation;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileReportDTO {

  private byte[] data;
  private String filename;

}

