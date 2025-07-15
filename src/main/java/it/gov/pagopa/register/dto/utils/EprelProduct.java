package it.gov.pagopa.register.dto.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  private String energyClassWash;
  private String ratedCapacity;
  private String ratedCapacityWash;
  private String totalVolume;
  private Cavities cavities;

  @Data
  public static class Cavities{
    private String volume;
  }
  public String getFieldValue(String fieldName) {
      return switch (fieldName) {
          case "productGroup" -> productGroup;
          case "energyClass" -> energyClass;
          case "orgVerificationStatus" -> orgVerificationStatus;
          case "trademarkVerificationStatus" -> trademarkVerificationStatus;
          case "blocked" -> blocked != null ? blocked.toString() : null;
          case "status" -> status;
          case null, default -> null;
      };
  }
}


