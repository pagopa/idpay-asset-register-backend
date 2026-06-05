package it.gov.pagopa.register.utils;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;


class EprelUtilsTest {

  @Test
  void testGenerateEprelUrl() {
    String url = EprelUtils.generateEprelUrl("WASHINGMACHINES", "12345");
    assertEquals("https://eprel.ec.europa.eu/screen/product/WASHINGMACHINES/12345", url);
  }


  @Test
  void testMapEnergyClass() {
    assertEquals("A+", EprelUtils.mapEnergyClass("AP"));
    assertEquals("A++", EprelUtils.mapEnergyClass("APP"));
    assertEquals("A+++", EprelUtils.mapEnergyClass("APPP"));

  }
}
