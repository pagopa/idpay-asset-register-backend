package it.gov.pagopa.register.dto.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetProductDTO {

  private String productFileId;

  private String status;

  private String uploadKey;

  private String elabTimeStamp;
}

