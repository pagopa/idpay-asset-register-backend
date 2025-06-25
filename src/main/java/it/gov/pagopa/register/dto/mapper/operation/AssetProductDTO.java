package it.gov.pagopa.register.dto.mapper.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetProductDTO {

  private String status;

  private String errorKey;

  private String elabTimeStamp;
}
