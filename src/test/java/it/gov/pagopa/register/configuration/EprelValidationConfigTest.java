package it.gov.pagopa.register.configuration;

import it.gov.pagopa.register.utils.EprelValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class EprelValidationConfigTest {

  private final EprelValidationConfig config = new EprelValidationConfig();


  @Nested
  @DisplayName("Configuration Initialization Tests")
  class ConfigurationInitializationTests {

    @Test
    @DisplayName("Should initialize with correct number of schemas")
    void shouldInitializeWithCorrectNumberOfSchemas() {
      LinkedHashMap<String, EprelValidationRule> schemas = config.getSchemas();
      assertEquals(6, schemas.size(), "Should contain exactly 6 validation rules");
    }

    @Test
    @DisplayName("Should contain all required validation rules")
    void shouldContainAllRequiredValidationRules() {
      LinkedHashMap<String, EprelValidationRule> schemas = config.getSchemas();
      System.out.println(schemas.keySet());
      assertTrue(schemas.containsKey(ORG_VERIFICATION_STATUS), "Should contain ORG_VERIFICATION_STATUS rule");
      assertTrue(schemas.containsKey(TRADE_MARKER_VERIFICATION_STATUS), "Should contain TRADE_MARKER_VERIFICATION_STATUS rule");
      assertTrue(schemas.containsKey(BLOCKED), "Should contain BLOCKED rule");
      assertTrue(schemas.containsKey(STATUS), "Should contain STATUS rule");
      assertTrue(schemas.containsKey(PRODUCT_GROUP), "Should contain PRODUCT_GROUP rule");
      assertTrue(schemas.containsKey(ENERGY_CLASS), "Should contain ENERGY_CLASS rule");
    }

    @Nested
    @DisplayName("Organization Verification Status Rule Tests")
    class OrgVerificationStatusRuleTests {

      private EprelValidationRule rule;

      @BeforeEach
      void setUp() {
        rule = config.getSchemas().get(ORG_VERIFICATION_STATUS);
      }

      @ParameterizedTest
      @ValueSource(strings = {"VERIFIED"})
      @DisplayName("Should pass for valid verification statuses")
      void shouldPassForValidVerificationStatuses(String status) {
        assertTrue(rule.getRule().test(status, null),
          "Should accept: " + status);
      }

      @ParameterizedTest
      @ValueSource(strings = {"UNVERIFIED"})
      @DisplayName("Should fail for invalid verification statuses")
      void shouldFailForInvalidVerificationStatuses(String status) {
        assertFalse(rule.getRule().test(status, null),
          "Should reject: " + status);
      }

      @Test
      @DisplayName("Should fail for null input")
      void shouldFailForNullInput() {
        assertFalse(rule.getRule().test(null, null),
          "Should reject null input");
      }

      @Test
      @DisplayName("Should have correct error message")
      void shouldHaveCorrectErrorMessage() {
        assertEquals("Lo stato di verifica dell'organizzazione non è VERIFICATO",
          rule.getMessage(), "Should have correct error message");
      }
    }

    @Nested
    @DisplayName("Trade Marker Verification Status Rule Tests")
    class TradeMarkerVerificationStatusRuleTests {

      private EprelValidationRule rule;

      @BeforeEach
      void setUp() {
        rule = config.getSchemas().get(TRADE_MARKER_VERIFICATION_STATUS);
      }

      @ParameterizedTest
      @ValueSource(strings = {"VERIFIED"})
      @DisplayName("Should pass for valid trade marker statuses")
      void shouldPassForValidTradeMarkerStatuses(String status) {
        assertTrue(rule.getRule().test(status, null),
          "Should accept: " + status);
      }

      @ParameterizedTest
      @ValueSource(strings = {"UNVERIFIED"})
      @DisplayName("Should fail for invalid trade marker statuses")
      void shouldFailForInvalidTradeMarkerStatuses(String status) {
        assertFalse(rule.getRule().test(status, null),
          "Should reject: " + status);
      }

      @Test
      @DisplayName("Should fail for null input")
      void shouldFailForNullInput() {
        assertFalse(rule.getRule().test(null, null),
          "Should reject null input");
      }

      @Test
      @DisplayName("Should have correct error message")
      void shouldHaveCorrectErrorMessage() {
        assertEquals("Lo stato di verifica del marchio non è VERIFICATO",
          rule.getMessage(), "Should have correct error message");
      }
    }

    @Nested
    @DisplayName("Blocked Rule Tests")
    class BlockedRuleTests {

      private EprelValidationRule rule;

      @BeforeEach
      void setUp() {
        rule = config.getSchemas().get(BLOCKED);
      }

      @ParameterizedTest
      @ValueSource(strings = {"FALSE"})
      @DisplayName("Should pass for FALSE values")
      void shouldPassForFalseValues(String value) {
        assertTrue(rule.getRule().test(value, null),
          "Should accept: " + value);
      }

      @ParameterizedTest
      @ValueSource(strings = {"TRUE", ""})
      @DisplayName("Should fail for non-FALSE values")
      void shouldFailForNonFalseValues(String value) {
        assertFalse(rule.getRule().test(value, null),
          "Should reject: " + value);
      }

      @Test
      @DisplayName("Should fail for null input")
      void shouldFailForNullInput() {
        assertFalse(rule.getRule().test(null, null),
          "Should reject null input");
      }

      @Test
      @DisplayName("Should have correct error message")
      void shouldHaveCorrectErrorMessage() {
        assertEquals("Il prodotto è BLOCCATO",
          rule.getMessage(), "Should have correct error message");
      }
    }

    @Nested
    @DisplayName("Status Rule Tests")
    class StatusRuleTests {

      private EprelValidationRule rule;

      @BeforeEach
      void setUp() {
        rule = config.getSchemas().get(STATUS);
      }

      @ParameterizedTest
      @ValueSource(strings = {"PUBLISHED"})
      @DisplayName("Should pass for PUBLISHED values")
      void shouldPassForPublishedValues(String value) {
        assertTrue(rule.getRule().test(value, null),
          "Should accept: " + value);
      }

      @ParameterizedTest
      @ValueSource(strings = {"DRAFT"})
      @DisplayName("Should fail for non-PUBLISHED values")
      void shouldFailForNonPublishedValues(String value) {
        assertFalse(rule.getRule().test(value, null),
          "Should reject: " + value);
      }

      @Test
      @DisplayName("Should fail for null input")
      void shouldFailForNullInput() {
        assertFalse(rule.getRule().test(null, null),
          "Should reject null input");
      }

      @Test
      @DisplayName("Should have correct error message")
      void shouldHaveCorrectErrorMessage() {
        assertEquals("Lo stato non è PUBBLICATO",
          rule.getMessage(), "Should have correct error message");
      }
    }

    @Nested
    @DisplayName("Product Group Rule Tests")
    class ProductGroupRuleTests {

      private EprelValidationRule rule;

      @BeforeEach
      void setUp() {
        rule = config.getSchemas().get(PRODUCT_GROUP);
      }

      @ParameterizedTest
      @CsvSource({
        "WASHINGMACHINES, washingmachines",
        "DISHWASHERS, dish",
        "OVENS, oven",
      })
      @DisplayName("Should pass when product group starts with expected prefix")
      void shouldPassWhenProductGroupStartsWithExpectedPrefix(String productGroup, String expectedPrefix) {
        assertTrue(rule.getRule().test(productGroup, expectedPrefix),
          "Product group '" + productGroup + "' should start with '" + expectedPrefix + "'");
      }

      @ParameterizedTest
      @CsvSource({
        "WASHINGMACHINES, DISH",
        "DISHWASHERS, WASHING",
        "OVENS, REFRIGERATOR",
        "TUMBLEDRYERS, OVEN"
      })
      @DisplayName("Should fail when product group doesn't start with expected prefix")
      void shouldFailWhenProductGroupDoesntStartWithExpectedPrefix(String productGroup, String expectedPrefix) {
        assertFalse(rule.getRule().test(productGroup, expectedPrefix),
          "Product group '" + productGroup + "' should not start with '" + expectedPrefix + "'");
      }

      @Test
      @DisplayName("Should fail for null product group")
      void shouldFailForNullProductGroup() {
        assertFalse(rule.getRule().test(null, "WASHING"),
          "Should reject null product group");
      }

      @Test
      @DisplayName("Should have correct error message")
      void shouldHaveCorrectErrorMessage() {
        assertEquals("La categoria EPREL non è compatibile con la categoria prevista",
          rule.getMessage(), "Should have correct error message");
      }
    }

    @Nested
    @DisplayName("Energy Class Rule Tests")
    class EnergyClassRuleTests {

      private EprelValidationRule rule;

      @BeforeEach
      void setUp() {
        rule = config.getSchemas().get(ENERGY_CLASS);
      }

      @ParameterizedTest
      @CsvSource({
        "WASHINGMACHINES, A",
        "WASHERDRIERS, A",
        "OVENS, A",
        "RANGEHOODS, B",
        "DISHWASHERS, C",
        "TUMBLEDRYERS, C",
        "REFRIGERATINGAPPL, D"
      })
      @DisplayName("Should pass for valid (productGourp,energy class)")
      void shouldPassWhenProductEnergyClassIsValid(String productGroup, String energyClass) {
        assertTrue(rule.getRule().test(energyClass, productGroup),
          "Should reject null energy class");
      }

      @Test
      @DisplayName("Should fail for null energy class")
      void shouldFailForNullEnergyClass() {
        assertFalse(rule.getRule().test(null, "A"),
          "Should reject null energy class");
      }

      @Test
      @DisplayName("Should have correct error message")
      void shouldHaveCorrectErrorMessage() {
        assertEquals("La classe energetica non è conforme",
          rule.getMessage(), "Should have correct error message");
      }
    }

  }
}
