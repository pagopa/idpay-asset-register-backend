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
    assertEquals("L", EprelUtils.mapEnergyClass("G"));
    assertEquals("X", EprelUtils.mapEnergyClass("X"));
    assertEquals("I", EprelUtils.mapEnergyClass("F"));
    assertEquals("H", EprelUtils.mapEnergyClass("E"));
    assertEquals("G", EprelUtils.mapEnergyClass("D"));
    assertEquals("F", EprelUtils.mapEnergyClass("C"));
    assertEquals("E", EprelUtils.mapEnergyClass("B"));
    assertEquals("D", EprelUtils.mapEnergyClass("A"));
    assertEquals("B", EprelUtils.mapEnergyClass("APP"));
    assertEquals("C", EprelUtils.mapEnergyClass("AP"));
    assertEquals("A", EprelUtils.mapEnergyClass("APPP"));

  }

  @Test
  void testMapEnergyClassInverse() {
    assertEquals("X", EprelUtils.mapEnergyClassInverse("X"));
    assertEquals("G", EprelUtils.mapEnergyClassInverse("L"));
    assertEquals("F", EprelUtils.mapEnergyClassInverse("I"));
    assertEquals("E", EprelUtils.mapEnergyClassInverse("H"));
    assertEquals("D", EprelUtils.mapEnergyClassInverse("G"));
    assertEquals("C", EprelUtils.mapEnergyClassInverse("F"));
    assertEquals("B", EprelUtils.mapEnergyClassInverse("E"));
    assertEquals("A", EprelUtils.mapEnergyClassInverse("D"));
    assertEquals("A+", EprelUtils.mapEnergyClassInverse("C"));
    assertEquals("A++", EprelUtils.mapEnergyClassInverse("B"));
    assertEquals("A+++", EprelUtils.mapEnergyClassInverse("A"));

  }
}
