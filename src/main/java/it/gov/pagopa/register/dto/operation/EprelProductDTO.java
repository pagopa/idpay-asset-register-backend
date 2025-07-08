package it.gov.pagopa.register.dto.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EprelProductDTO {

  private String eprelRegistrationNumber;
  private String productGroup;
  private String supplierOrTrademark;
  private String modelIdentifier;
  private String energyClass;
  private String orgVerificationStatus;
  private String trademarkVerificationStatus;
  private Boolean blocked;
  private String status;
  private String energyClassWash;

}
