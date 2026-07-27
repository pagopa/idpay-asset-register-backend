package it.gov.pagopa.register.utils;



public class EprelUtils {

  private EprelUtils(){
  }

  public static String generateEprelUrl(String productGroup, String eprelCode) {
    if (productGroup == null || eprelCode == null) return null;
    return String.format("https://eprel.ec.europa.eu/screen/product/%s/%s", productGroup, eprelCode);
  }

  public static String mapEnergyClass(String value) {
    if (value == null) return null;
    return switch (value) {
      case "AP" -> "A+";
      case "APP" -> "A++";
      case "APPP" -> "A+++";
      default -> value;
    };
  }


}
