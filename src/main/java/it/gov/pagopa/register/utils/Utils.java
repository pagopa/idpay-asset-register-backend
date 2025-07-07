package it.gov.pagopa.register.utils;


import org.apache.commons.lang3.StringUtils;

public class Utils {

  private Utils(){
  }

  public static String generateEprelUrl(String productGroup, String eprelCode) {
    if (StringUtils.isBlank(productGroup) || StringUtils.isBlank(eprelCode)) {
      return null;
    }
    return String.format("https://eprel.ec.europa.eu/screen/product/%s/%s", productGroup, eprelCode);
  }

}
