package it.gov.pagopa.register.configuration;

import it.gov.pagopa.register.utils.ColumnValidationRule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductFileValidationConfigTest {

  private ProductFileValidationConfig config;

  @BeforeAll
  void setUp() {
    config = new ProductFileValidationConfig();
  }

  @Test
  @DisplayName("Should initialize with correct number of schemas")
  void shouldInitializeWithCorrectNumberOfSchemas() {
    Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = config.getSchemas();
    assertEquals(2, schemas.size(), "Should contain exactly 2 schemas");
  }

  @Test
  @DisplayName("Should contain cookinghobs and eprel schemas")
  void shouldContainExpectedSchemas() {
    Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = config.getSchemas();
    assertTrue(schemas.containsKey("cookinghobs"), "Should contain 'cookinghobs' schema");
    assertTrue(schemas.containsKey("eprel"), "Should contain 'eprel' schema");
  }

  @Nested
  @DisplayName("Cookinghobs Schema Tests")
  class CookinghobsSchemaTests {

    private LinkedHashMap<String, ColumnValidationRule> schema;

    @BeforeEach
    void init() {
      schema = config.getSchemas().get("cookinghobs");
    }

    @Test
    @DisplayName("Should contain all required columns")
    void shouldContainAllRequiredColumns() {
      assertTrue(schema.containsKey(CODE_GTIN_EAN));
      assertTrue(schema.containsKey(CODE_PRODUCT));
      assertTrue(schema.containsKey(CATEGORY));
      assertTrue(schema.containsKey(COUNTRY_OF_PRODUCTION));
      assertTrue(schema.containsKey(BRAND));
      assertTrue(schema.containsKey(MODEL));
    }
  }

  @Nested
  @DisplayName("Eprel Schema Tests")
  class EprelSchemaTests {

    private LinkedHashMap<String, ColumnValidationRule> schema;

    @BeforeEach
    void init() {
      schema = config.getSchemas().get("eprel");
    }

    @Test
    @DisplayName("Should contain all required columns")
    void shouldContainAllRequiredColumns() {
      assertTrue(schema.containsKey(CODE_EPREL));
      assertTrue(schema.containsKey(CODE_GTIN_EAN));
      assertTrue(schema.containsKey(CODE_PRODUCT));
      assertTrue(schema.containsKey(CATEGORY));
      assertTrue(schema.containsKey(COUNTRY_OF_PRODUCTION));
    }
  }
}
