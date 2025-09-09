package it.gov.pagopa.register.dto.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
  private List<Cavity> cavities;
  private List<RefrigeratorCompartment> compartments;

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Cavity {
    private Integer volume;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class RefrigeratorCompartment {

    private String volume;
    private String compartmentType;
    private List<SubCompartment> subCompartments;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class SubCompartment {

    private String subCompartmentType;
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


