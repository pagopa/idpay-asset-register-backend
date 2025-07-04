package it.gov.pagopa.register.utils;

public class Utils {

  private Utils(){
  }

  public static String generateEprelUrl(String productGroup, String eprelCode) {
    if (productGroup == null || eprelCode == null) return null;
    return String.format("https://eprel.ec.europa.eu/screen/product/%s/%s", productGroup, eprelCode);
  }

}
