package it.gov.pagopa.register.dto.operation;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;

@Data
@Builder
public class FileReportDTO {

  private byte[] data;
  private String filename;

}

