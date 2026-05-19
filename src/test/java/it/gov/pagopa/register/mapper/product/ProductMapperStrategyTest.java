package it.gov.pagopa.register.mapper.product;

import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.operation.Product;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductMapperStrategyTest {

  private static final String ORG_ID = "org1";
  private static final String INITIATIVE_ID = "initiative1";
  private static final String PRODUCT_FILE_ID = "file1";
  private static final String ORGANIZATION_NAME = "Organization";

  private final CookingHobsProductMapper cookingHobsProductMapper = new CookingHobsProductMapper();
  private final DecoderProductMapper decoderProductMapper = new DecoderProductMapper();
  private final EprelProductMapper eprelProductMapper = new EprelProductMapper();

  @Test
  void cookingHobsExtractBusinessKeyReadsConfiguredField() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    CategoryConfig categoryConfig = new CategoryConfig();
    categoryConfig.setInputIdentifierField(CODE_GTIN_EAN);
    when(csvRecord.get(CODE_GTIN_EAN)).thenReturn(" 800123 ");

    assertEquals(" 800123 ", cookingHobsProductMapper.extractBusinessKey(csvRecord, categoryConfig));
  }

  @Test
  void cookingHobsMapToProductBuildsUploadedProduct() {
    CSVRecord csvRecord = cookingHobsRecord();

    Product product = cookingHobsProductMapper.mapToProduct(
      csvRecord,
      COOKINGHOBS,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      null
    );

    assertEquals("8001234567890_" + INITIATIVE_ID, product.getId());
    assertEquals("8001234567890", product.getGtinCode());
    assertEquals("PROD123", product.getProductCode());
    assertEquals(COOKINGHOBS, product.getCategory());
    assertEquals("Italy", product.getCountryOfProduction());
    assertEquals("BrandX", product.getBrand());
    assertEquals("ModelX", product.getModel());
    assertEquals("N\\A", product.getCapacity());
    assertEquals(ProductStatus.UPLOADED.name(), product.getStatus());
    assertEquals(ORGANIZATION_NAME, product.getOrganizationName());
    assertEquals("Piano cottura BrandX ModelX", product.getProductName());
    assertEquals("8001234567890 - Piano cottura BrandX ModelX", product.getFullProductName());
    assertNotNull(product.getRegistrationDate());
    assertNotNull(product.getStatusChangeChronology());
    assertTrue(product.getStatusChangeChronology().isEmpty());
    assertEquals("", product.getFormalMotivation());
  }

  @Test
  void cookingHobsMapToCsvRowWritesExpectedColumns() {
    Product product = Product.builder()
      .gtinCode("GTIN1")
      .productCode("PROD1")
      .category(COOKINGHOBS)
      .countryOfProduction("Italy")
      .model("ModelX")
      .brand("BrandX")
      .build();

    CSVRecord csvRecord = cookingHobsProductMapper.mapToCsvRow(
      product,
      List.of("gtinCode", "productCode", "category", "countryOfProduction", "model", "brand")
    );

    assertNotNull(csvRecord);
    assertEquals("GTIN1", csvRecord.get(0));
    assertEquals("PROD1", csvRecord.get(1));
    assertEquals(COOKINGHOBS, csvRecord.get(2));
    assertEquals("Italy", csvRecord.get(3));
    assertEquals("ModelX", csvRecord.get(4));
    assertEquals("BrandX", csvRecord.get(5));
  }

  @Test
  void cookingHobsMapToCsvRowReturnsNullWhenHeadersAreInvalid() {
    Product product = Product.builder().build();

    assertNull(cookingHobsProductMapper.mapToCsvRow(product, null));
  }

  @Test
  void decoderExtractBusinessKeyReadsConfiguredField() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    CategoryConfig categoryConfig = new CategoryConfig();
    categoryConfig.setInputIdentifierField(CODE_PRODUCT);
    when(csvRecord.get(CODE_PRODUCT)).thenReturn(" DEC-1 ");

    assertEquals(" DEC-1 ", decoderProductMapper.extractBusinessKey(csvRecord, categoryConfig));
  }

  @Test
  void decoderMapToProductUsesRequestedCategory() {
    CSVRecord csvRecord = decoderRecord();

    Product product = decoderProductMapper.mapToProduct(
      csvRecord,
      SATELLITE,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      null
    );

    assertEquals("8009876543210_" + INITIATIVE_ID, product.getId());
    assertEquals("8009876543210", product.getGtinCode());
    assertEquals("DEC123", product.getProductCode());
    assertEquals(SATELLITE, product.getCategory());
    assertNull(product.getCountryOfProduction());
    assertEquals("DecoderBrand", product.getBrand());
    assertEquals("DecoderModel", product.getModel());
    assertEquals("N\\A", product.getCapacity());
    assertEquals(ProductStatus.UPLOADED.name(), product.getStatus());
    assertEquals("Satellitare DecoderBrand DecoderModel", product.getProductName());
    assertEquals("8009876543210 - Satellitare DecoderBrand DecoderModel", product.getFullProductName());
  }

  @Test
  void decoderMapToCsvRowWritesExpectedColumns() {
    Product product = Product.builder()
      .gtinCode("GTIN2")
      .productCode("PROD2")
      .category(SATELLITE)
      .model("ModelY")
      .brand("BrandY")
      .build();

    CSVRecord csvRecord = decoderProductMapper.mapToCsvRow(
      product,
      List.of("gtinCode", "productCode", "category", "model", "brand")
    );

    assertNotNull(csvRecord);
    assertEquals("GTIN2", csvRecord.get(0));
    assertEquals("PROD2", csvRecord.get(1));
    assertEquals(SATELLITE, csvRecord.get(2));
    assertEquals("ModelY", csvRecord.get(3));
    assertEquals("BrandY", csvRecord.get(4));
  }

  @Test
  void decoderMapToCsvRowReturnsNullWhenHeadersAreInvalid() {
    Product product = Product.builder().build();

    assertNull(decoderProductMapper.mapToCsvRow(product, null));
  }

  @Test
  void eprelExtractBusinessKeyNormalizesConfiguredField() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    CategoryConfig categoryConfig = new CategoryConfig();
    categoryConfig.setInputIdentifierField(CODE_GTIN_EAN);
    when(csvRecord.get(CODE_GTIN_EAN)).thenReturn(" 800 123 ");

    assertEquals("800123", eprelProductMapper.extractBusinessKey(csvRecord, categoryConfig));
  }

  @ParameterizedTest
  @MethodSource("eprelCapacityCases")
  void eprelMapToProductMapsCategorySpecificCapacity(String category, Map<String, Object> extraData, String expectedCapacity) {
    CSVRecord csvRecord = eprelRecord();

    Product product = eprelProductMapper.mapToProduct(
      csvRecord,
      category,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      new MappingContext(extraData)
    );

    String normalizedCategory = category.trim().replaceAll("\\s+", "");
    assertEquals("8001112223334_" + INITIATIVE_ID, product.getId());
    assertEquals("8001112223334", product.getGtinCode());
    assertEquals("EPREL123", product.getEprelCode());
    assertEquals("PROD999", product.getProductCode());
    assertEquals(normalizedCategory, product.getCategory());
    assertEquals("GroupA", product.getProductGroup());
    assertEquals("Italy", product.getCountryOfProduction());
    assertEquals("BrandE", product.getBrand());
    assertEquals("ModelE", product.getModel());
    assertEquals("A", product.getEnergyClass());
    assertEquals(expectedCapacity, product.getCapacity());
    assertEquals(ProductStatus.UPLOADED.name(), product.getStatus());
    assertEquals(ORGANIZATION_NAME, product.getOrganizationName());
    assertTrue(product.getProductName().contains("BrandE ModelE"));
    assertTrue(product.getFullProductName().startsWith("8001112223334 - "));
  }

  @Test
  void eprelMapToProductUsesFreezerNameWhenRefrigeratingCompartmentsAreNotFridge() {
    CSVRecord csvRecord = eprelRecord();
    EprelProduct.RefrigeratorCompartment compartment = EprelProduct.RefrigeratorCompartment.builder()
      .compartmentType("FREEZER")
      .build();

    Product product = eprelProductMapper.mapToProduct(
      csvRecord,
      REFRIGERATINGAPPL,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      new MappingContext(baseEprelData(Map.of(
        "totalVolume", "300",
        "compartments", List.of(compartment)
      )))
    );

    assertEquals("300 l", product.getCapacity());
    assertTrue(product.getProductName().startsWith(FREEZER_IT));
  }

  @Test
  void eprelMapToProductUsesRefrigeratorNameWhenCompartmentIsFridgeCategory() {
    CSVRecord csvRecord = eprelRecord();
    EprelProduct.RefrigeratorCompartment compartment = EprelProduct.RefrigeratorCompartment.builder()
      .compartmentType(CELLAR)
      .build();

    Product product = eprelProductMapper.mapToProduct(
      csvRecord,
      REFRIGERATINGAPPL,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      new MappingContext(baseEprelData(Map.of(
        "totalVolume", "300",
        "compartments", List.of(compartment)
      )))
    );

    assertEquals("300 l", product.getCapacity());
    assertTrue(product.getProductName().startsWith(REFRIGERATOR_IT));
  }

  @Test
  void eprelMapToProductUsesRefrigeratorNameWhenVariableTempContainsFridgeSubCompartment() {
    CSVRecord csvRecord = eprelRecord();
    EprelProduct.SubCompartment subCompartment = EprelProduct.SubCompartment.builder()
      .compartmentType("CELLAR")
      .build();
    EprelProduct.RefrigeratorCompartment compartment = EprelProduct.RefrigeratorCompartment.builder()
      .compartmentType(VARIABLE_TEMP)
      .subCompartments(List.of(subCompartment))
      .build();

    Product product = eprelProductMapper.mapToProduct(
      csvRecord,
      REFRIGERATINGAPPL,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      new MappingContext(baseEprelData(Map.of(
        "totalVolume", "300",
        "compartments", List.of(compartment)
      )))
    );

    assertEquals("300 l", product.getCapacity());
    assertTrue(product.getProductName().startsWith(REFRIGERATOR_IT));
  }

  @Test
  void eprelMapToProductUsesFreezerNameWhenVariableTempHasNoSubCompartments() {
    CSVRecord csvRecord = eprelRecord();
    EprelProduct.RefrigeratorCompartment compartment = EprelProduct.RefrigeratorCompartment.builder()
      .compartmentType(VARIABLE_TEMP)
      .subCompartments(null)
      .build();

    Product product = eprelProductMapper.mapToProduct(
      csvRecord,
      REFRIGERATINGAPPL,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      new MappingContext(baseEprelData(Map.of(
        "totalVolume", "300",
        "compartments", List.of(compartment)
      )))
    );

    assertEquals("300 l", product.getCapacity());
    assertTrue(product.getProductName().startsWith(FREEZER_IT));
  }

  @Test
  void eprelMapToProductUsesFreezerNameWhenVariableTempSubCompartmentIsNotFridge() {
    CSVRecord csvRecord = eprelRecord();
    EprelProduct.SubCompartment subCompartment = EprelProduct.SubCompartment.builder()
      .compartmentType("FREEZER")
      .build();
    EprelProduct.RefrigeratorCompartment compartment = EprelProduct.RefrigeratorCompartment.builder()
      .compartmentType(VARIABLE_TEMP)
      .subCompartments(List.of(subCompartment))
      .build();

    Product product = eprelProductMapper.mapToProduct(
      csvRecord,
      REFRIGERATINGAPPL,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      new MappingContext(baseEprelData(Map.of(
        "totalVolume", "300",
        "compartments", List.of(compartment)
      )))
    );

    assertEquals("300 l", product.getCapacity());
    assertTrue(product.getProductName().startsWith(FREEZER_IT));
  }

  @Test
  void eprelMapToProductUsesFreezerNameWhenRefrigeratingCompartmentsAreMissing() {
    CSVRecord csvRecord = eprelRecord();

    Product product = eprelProductMapper.mapToProduct(
      csvRecord,
      REFRIGERATINGAPPL,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      new MappingContext(baseEprelData(Map.of("totalVolume", "300")))
    );

    assertEquals("300 l", product.getCapacity());
    assertTrue(product.getProductName().startsWith(FREEZER_IT));
  }

  @Test
  void eprelMapToProductDoesNotPrefixFullNameWhenGtinIsBlank() {
    CSVRecord csvRecord = eprelRecordWithBlankGtin();

    Product product = eprelProductMapper.mapToProduct(
      csvRecord,
      WASHINGMACHINES,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      new MappingContext(baseEprelData(Map.of("ratedCapacity", "8")))
    );

    assertEquals("", product.getGtinCode());
    assertEquals(product.getProductName(), product.getFullProductName());
  }

  @Test
  void eprelMapToProductThrowsWhenCategoryIsNull() {
    CSVRecord csvRecord = eprelRecord();

    assertThrows(NullPointerException.class, () -> eprelProductMapper.mapToProduct(
      csvRecord,
      null,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      new MappingContext(baseEprelData(Map.of()))
    ));
  }

  @Test
  void eprelMapToProductThrowsWhenContextIsMissing() {
    CSVRecord csvRecord = eprelRecord();

    assertThrows(NullPointerException.class, () -> eprelProductMapper.mapToProduct(
      csvRecord,
      WASHINGMACHINES,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      null
    ));
  }

  @Test
  void eprelMapToCsvRowWritesExpectedColumns() {
    Product product = Product.builder()
      .eprelCode("EPREL321")
      .gtinCode("GTIN3")
      .productCode("PROD3")
      .category(WASHINGMACHINES)
      .countryOfProduction("Italy")
      .build();

    CSVRecord csvRecord = eprelProductMapper.mapToCsvRow(
      product,
      List.of("eprelCode", "gtinCode", "productCode", "category", "countryOfProduction")
    );

    assertNotNull(csvRecord);
    assertEquals("EPREL321", csvRecord.get(0));
    assertEquals("GTIN3", csvRecord.get(1));
    assertEquals("PROD3", csvRecord.get(2));
    assertEquals(WASHINGMACHINES, csvRecord.get(3));
    assertEquals("Italy", csvRecord.get(4));
  }

  @Test
  void eprelMapToCsvRowReturnsNullWhenHeadersAreInvalid() {
    Product product = Product.builder().build();

    assertNull(eprelProductMapper.mapToCsvRow(product, null));
  }

  static Stream<Arguments> eprelCapacityCases() {
    EprelProduct.Cavity cavity = EprelProduct.Cavity.builder()
      .volume(65)
      .build();
    EprelProduct.Cavity cavityWithNullVolume = EprelProduct.Cavity.builder()
      .volume(null)
      .build();

    return Stream.of(
      Arguments.of(WASHINGMACHINES, baseEprelData(Map.of("ratedCapacity", "8")), "8 kg"),
      Arguments.of(WASHINGMACHINES, baseEprelData(Map.of()), "N\\A"),
      Arguments.of(TUMBLEDRYERS, baseEprelData(Map.of("ratedCapacity", "7")), "7 kg"),
      Arguments.of(TUMBLEDRYERS, baseEprelData(Map.of()), "N\\A"),
      Arguments.of(WASHERDRIERS, baseEprelData(Map.of("ratedCapacityWash", "6")), "6 kg"),
      Arguments.of(WASHERDRIERS, baseEprelData(Map.of()), "N\\A"),
      Arguments.of(OVENS, baseEprelData(Map.of("cavities", List.of(cavity, cavityWithNullVolume))), "65 l / N/A"),
      Arguments.of(OVENS, baseEprelData(Map.of("cavities", List.of())), "N\\A"),
      Arguments.of(OVENS, baseEprelData(Map.of()), "N\\A"),
      Arguments.of(DISHWASHERS, baseEprelData(Map.of("ratedCapacity", "12")), "12 c"),
      Arguments.of(DISHWASHERS, baseEprelData(Map.of()), "N\\A"),
      Arguments.of(REFRIGERATINGAPPL, baseEprelData(Map.of("totalVolume", "250", "compartments", List.of())), "250 l"),
      Arguments.of(REFRIGERATINGAPPL, baseEprelData(Map.of("compartments", List.of())), "N\\A"),
      Arguments.of(RANGEHOODS, baseEprelData(Map.of()), "N\\A"),
      Arguments.of(" WASHINGMACHINES ", baseEprelData(Map.of()), "N\\A")
    );
  }

  private static CSVRecord cookingHobsRecord() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get(CODE_PRODUCT)).thenReturn(" PROD 123 ");
    when(csvRecord.get(CODE_GTIN_EAN)).thenReturn(" 800 1234567890 ");
    when(csvRecord.get(COUNTRY_OF_PRODUCTION)).thenReturn("Italy");
    when(csvRecord.get(BRAND)).thenReturn("BrandX");
    when(csvRecord.get(MODEL)).thenReturn("ModelX");
    return csvRecord;
  }

  private static CSVRecord decoderRecord() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get(CODE_PRODUCT)).thenReturn(" DEC 123 ");
    when(csvRecord.get(CODE_GTIN_EAN)).thenReturn(" 800 9876543210 ");
    when(csvRecord.get(BRAND)).thenReturn("DecoderBrand");
    when(csvRecord.get(MODEL)).thenReturn("DecoderModel");
    return csvRecord;
  }

  private static CSVRecord eprelRecord() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get(CODE_PRODUCT)).thenReturn(" PROD 999 ");
    when(csvRecord.get(CODE_GTIN_EAN)).thenReturn(" 800 1112223334 ");
    when(csvRecord.get(CODE_EPREL)).thenReturn("EPREL123");
    when(csvRecord.get(COUNTRY_OF_PRODUCTION)).thenReturn("Italy");
    return csvRecord;
  }

  private static CSVRecord eprelRecordWithBlankGtin() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get(CODE_PRODUCT)).thenReturn(" PROD 999 ");
    when(csvRecord.get(CODE_GTIN_EAN)).thenReturn("   ");
    when(csvRecord.get(CODE_EPREL)).thenReturn("EPREL123");
    when(csvRecord.get(COUNTRY_OF_PRODUCTION)).thenReturn("Italy");
    return csvRecord;
  }

  private static Map<String, Object> baseEprelData(Map<String, Object> values) {
    java.util.HashMap<String, Object> data = new java.util.HashMap<>();
    data.put("productGroup", "GroupA");
    data.put("supplierOrTrademark", "BrandE");
    data.put("modelIdentifier", "ModelE");
    data.put("energyClass", "A");
    data.putAll(values);
    return data;
  }

}
