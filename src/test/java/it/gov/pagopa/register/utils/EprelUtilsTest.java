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
  void testIsEnergyClassValid_ValidCases() {
    assertTrue(EprelUtils.isEnergyClassValid("A", "WASHINGMACHINES")); // A <= A
    assertTrue(EprelUtils.isEnergyClassValid("AP", "WASHINGMACHINES")); // AP < A
    assertTrue(EprelUtils.isEnergyClassValid("APP", "DISHWASHERS")); // APP < C
  }

  @Test
  void testIsEnergyClassValid_InvalidCases() {
    assertFalse(EprelUtils.isEnergyClassValid("D", "WASHINGMACHINES")); // D > A
    assertFalse(EprelUtils.isEnergyClassValid("Z", "OVENS")); // Not in order
    assertFalse(EprelUtils.isEnergyClassValid("A", "UNKNOWN")); // Unknown category
    assertFalse(EprelUtils.isEnergyClassValid(null, "OVENS")); // Null energy class
    assertFalse(EprelUtils.isEnergyClassValid("A", null)); // Null category
    assertFalse(EprelUtils.isEnergyClassValid(" ", "OVENS")); // Blank energy class
  }

  @Test
  void testMapEnergyClass() {
    assertEquals("A+", EprelUtils.mapEnergyClass("AP"));
    assertEquals("A++", EprelUtils.mapEnergyClass("APP"));
    assertEquals("A+++", EprelUtils.mapEnergyClass("APPP"));

  }
}
