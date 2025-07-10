package it.gov.pagopa.register.utils;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.ENERGY_CLASS_ORDER;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.ENERGY_CLASS_REQUIREMENTS;

public class EprelUtils {

  private EprelUtils(){
  }

  public static String generateEprelUrl(String productGroup, String eprelCode) {
    if (productGroup == null || eprelCode == null) return null;
    return String.format("https://eprel.ec.europa.eu/screen/product/%s/%s", productGroup, eprelCode);
  }


  public static Boolean isEnergyClassValid(String energyClass, String category) {
    if (energyClass == null || energyClass.isBlank()) {
      return false;
    }

    String requiredMinClass = ENERGY_CLASS_REQUIREMENTS.get(category.toUpperCase());
    if (requiredMinClass == null) {
      return false;
    }

    int requiredIndex = ENERGY_CLASS_ORDER.indexOf(requiredMinClass);    int productIndex = ENERGY_CLASS_ORDER.indexOf(energyClass.toUpperCase());



    if (requiredIndex == -1 || productIndex == -1) {
      return false;
    }

    return productIndex <= requiredIndex;
  }

  public static String mapEnergyClass(String value) {
    return switch (value) {
      case "AP" -> "A+";
      case "APP" -> "A++";
      case "APPP" -> "A+++";
      default -> value;
    };
  }



}
