package it.gov.pagopa.register.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ProductCategoriesTest {

  @Test
  void values_includeApplianceAndDecoderCategories() {
    assertTrue(Arrays.asList(ProductCategories.values()).contains(ProductCategories.WASHINGMACHINES));
    assertTrue(Arrays.asList(ProductCategories.values()).contains(ProductCategories.COOKINGHOBS));
    assertTrue(Arrays.asList(ProductCategories.values()).contains(ProductCategories.DS));
    assertTrue(Arrays.asList(ProductCategories.values()).contains(ProductCategories.DTSC));
  }

  @Test
  void valueOf_resolvesCategoryByName() {
    assertEquals(ProductCategories.DISHWASHERS, ProductCategories.valueOf("DISHWASHERS"));
  }
}
