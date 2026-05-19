package it.gov.pagopa.register.dto.utils;

import it.gov.pagopa.register.model.operation.Product;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DtoUtilsTest {

  @Test
  void columnValidationRule_delegatesToPredicate() {
    ColumnValidationRule rule = new ColumnValidationRule(String::startsWith, "invalid");

    assertTrue(rule.isValid("COOKINGHOBS-1", "COOKINGHOBS"));
    assertFalse(rule.isValid("OTHER-1", "COOKINGHOBS"));
    assertEquals("invalid", rule.getMessage());
  }

  @Test
  void eprelValidationRule_delegatesToPredicate() {
    EprelValidationRule rule = new EprelValidationRule(String::equalsIgnoreCase, "invalid");

    assertTrue(rule.isValid("loaded", "LOADED"));
    assertEquals("invalid", rule.getMessage());
  }

  @Test
  void eprelProduct_getFieldValueReturnsKnownFieldsAndNullForUnknown() {
    EprelProduct product = EprelProduct.builder()
      .productGroup("group")
      .energyClass("A")
      .orgVerificationStatus("verified")
      .trademarkVerificationStatus("valid")
      .blocked(Boolean.FALSE)
      .status("PUBLISHED")
      .build();

    assertEquals("group", product.getFieldValue("productGroup"));
    assertEquals("A", product.getFieldValue("energyClass"));
    assertEquals("verified", product.getFieldValue("orgVerificationStatus"));
    assertEquals("valid", product.getFieldValue("trademarkVerificationStatus"));
    assertEquals("false", product.getFieldValue("blocked"));
    assertEquals("PUBLISHED", product.getFieldValue("status"));
    assertNull(product.getFieldValue("unknown"));
    assertNull(product.getFieldValue(null));
  }

  @Test
  void eprelProduct_nestedDtosExposeValues() {
    EprelProduct.Cavity cavity = EprelProduct.Cavity.builder().volume(10).build();
    EprelProduct.SubCompartment subCompartment = EprelProduct.SubCompartment.builder()
      .compartmentType("FREEZER")
      .build();
    EprelProduct.RefrigeratorCompartment compartment = EprelProduct.RefrigeratorCompartment.builder()
      .volume("20")
      .compartmentType("FRESH_FOOD")
      .subCompartments(List.of(subCompartment))
      .build();

    EprelProduct product = EprelProduct.builder()
      .cavities(List.of(cavity))
      .compartments(List.of(compartment))
      .build();

    assertEquals(10, product.getCavities().getFirst().getVolume());
    assertEquals("20", product.getCompartments().getFirst().getVolume());
    assertEquals("FREEZER", product.getCompartments().getFirst().getSubCompartments().getFirst().getCompartmentType());
  }

  @Test
  void eventDetailsAndProductValidationResultExposeConstructorValues() {
    EventDetails eventDetails = new EventDetails("org", "category", "file", "organization", "initiative");
    Product product = Product.builder().gtinCode("gtin").build();
    CSVRecord csvRecord = mock(CSVRecord.class);
    ProductValidationResult result = new ProductValidationResult(Map.of("gtin", product), List.of(csvRecord), Map.of(csvRecord, "error"));

    assertEquals("org", eventDetails.getOrgId());
    assertEquals("category", eventDetails.getCategory());
    assertEquals("file", eventDetails.getProductFileId());
    assertEquals("organization", eventDetails.getOrganizationName());
    assertEquals("initiative", eventDetails.getInitiativeId());
    assertEquals(product, result.getValidRecords().get("gtin"));
    assertEquals(List.of(csvRecord), result.getInvalidRecords());
    assertEquals("error", result.getErrorMessages().get(csvRecord));
  }
}
