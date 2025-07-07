package it.gov.pagopa.register.utils;

import java.lang.reflect.Field;

import static it.gov.pagopa.register.constants.RegisterConstants.ENERGY_CLASS_ORDER;
import static it.gov.pagopa.register.constants.RegisterConstants.ENERGY_CLASS_REQUIREMENTS;

public class Utils {

  private Utils(){
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




}
