package it.gov.pagopa.register.dto.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetProductDTO {

  private String productFileId;

  private String status;

  private String uploadKey;

  private String elabTimeStamp;
}

