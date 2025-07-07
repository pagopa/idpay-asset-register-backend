package it.gov.pagopa.register.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EprelProduct {

  private String eprelRegistrationNumber;
  private String productGroup;
  private String supplierOrTrademark;
  private String modelIdentifier;
  private String energyClass;
  private String orgVerificationStatus;
  private String trademarkVerificationStatus;
  private Boolean blocked;
  private String status;


  public String getFieldValue(String fieldName) {
      return switch (fieldName) {
          case "eprelRegistrationNumber" -> eprelRegistrationNumber;
          case "productGroup" -> productGroup;
          case "supplierOrTrademark" -> supplierOrTrademark;
          case "modelIdentifier" -> modelIdentifier;
          case "energyClass" -> energyClass;
          case "orgVerificationStatus" -> orgVerificationStatus;
          case "trademarkVerificationStatus" -> trademarkVerificationStatus;
          case "blocked" -> blocked != null ? blocked.toString() : null;
          case "status" -> status;
          case null, default -> null;
      };
  }
}


