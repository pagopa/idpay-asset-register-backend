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

    int requiredIndex = ENERGY_CLASS_ORDER.indexOf(requiredMinClass);
    int productIndex = ENERGY_CLASS_ORDER.indexOf(energyClass.toUpperCase());



    if (requiredIndex == -1 || productIndex == -1) {
      return false;
    }

    return productIndex <= requiredIndex;
  }

  public static String mapEnergyClass(String value) {
    if (value == null) return null;
    return switch (value) {
      case "G" -> "L";
      case "F" -> "I";
      case "E" -> "H";
      case "D" -> "G";
      case "C" -> "F";
      case "B" -> "E";
      case "A" -> "D";
      case "AP" -> "C";
      case "APP" -> "B";
      case "APPP" -> "A";
      default -> value;
    };
  }

  public static String mapEnergyClassInverse(String value) {
    if (value == null) return null;
    return switch (value) {
      case "L" -> "G";
      case "I" -> "F";
      case "H" -> "E";
      case "G" -> "D";
      case "F" -> "C";
      case "E" -> "B";
      case "D" -> "A";
      case "C" -> "A+";
      case "B" -> "A++";
      case "A" -> "A+++";
      default -> value;
    };
  }

}
