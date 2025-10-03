package it.gov.pagopa.register.mapper;

import it.gov.pagopa.register.dto.operation.FormalMotivationDTO;
import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.mapper.operation.ProductMapper;
import it.gov.pagopa.register.model.operation.Product;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static it.gov.pagopa.register.utils.ObjectMaker.buildStatusChangeEventsList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductMapperTest {

  // ---------- toDTO ----------

  @Test
  void testToDTO_NullEntity() {
    assertNull(ProductMapper.toDTO(null, null));
  }

  @Test
  void testToDTO_RoleOperatore_StatusDowngraded_AndChronologyHidden() {
    Product product = Product.builder()
      .organizationId("org1")
      .registrationDate(LocalDateTime.now())
      .status(ProductStatus.WAIT_APPROVED.name())
      .model("ModelX")
      .productGroup("GroupA")
      .category("CategoryA")
      .brand("BrandX")
      .eprelCode("EPREL123")
      .gtinCode("GTIN123")
      .productCode("PROD123")
      .countryOfProduction("Italy")
      .energyClass("A")
      .capacity("10")
      .productFileId("file123")
      .statusChangeChronology(buildStatusChangeEventsList())
      .productName("CategoryA BrandX ModelX 10")
      .formalMotivation(new FormalMotivationDTO("OK", LocalDateTime.now()))
      .organizationName("orgName")
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.OPERATORE.getRole());

    assertNotNull(dto);
    assertEquals(ProductStatus.UPLOADED.name(), dto.getStatus());
    assertNotNull(dto.getStatusChangeChronology(), "Per OPERATORE deve essere lista vuota, non null");
    assertTrue(dto.getStatusChangeChronology().isEmpty(), "La chronology deve essere nascosta come lista vuota");
    assertEquals("OK", dto.getFormalMotivation().getFormalMotivation());
  }

  @Test
  void testToDTO_RoleInvitalia_StatusUnchanged_AndChronologyVisible() {
    Product product = Product.builder()
      .organizationId("org1")
      .registrationDate(LocalDateTime.now())
      .status(ProductStatus.WAIT_APPROVED.name())
      .model("ModelX")
      .productGroup("GroupA")
      .category("CategoryA")
      .brand("BrandX")
      .eprelCode("EPREL123")
      .gtinCode("GTIN123")
      .productCode("PROD123")
      .countryOfProduction("Italy")
      .energyClass("A")
      .capacity("10")
      .productFileId("file123")
      .productName("CategoryA BrandX ModelX 10")
      .statusChangeChronology(buildStatusChangeEventsList())
      .formalMotivation(new FormalMotivationDTO("Motivo", LocalDateTime.now()))
      .organizationName("orgName")
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA_ADMIN.getRole());
    assertEquals(ProductStatus.WAIT_APPROVED.name(), dto.getStatus());
    assertNotNull(dto.getStatusChangeChronology());
    assertEquals("Motivo", dto.getFormalMotivation().getFormalMotivation());
  }

  @Test
  void testToDTO_FormalMotivationNull_ObjectReplacedWithDefault() {
    Product product = Product.builder()
      .organizationId("org1")
      .registrationDate(LocalDateTime.now())
      .status(ProductStatus.APPROVED.name())
      .model("M")
      .productGroup("G")
      .category("C")
      .brand("B")
      .capacity("10")
      .formalMotivation(null)
      .organizationName("orgName")
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA_ADMIN.getRole());
    assertNotNull(dto.getFormalMotivation(), "Deve valorizzare un FormalMotivationDTO di default");
    assertEquals("", dto.getFormalMotivation().getFormalMotivation());
    assertEquals(LocalDateTime.MIN, dto.getFormalMotivation().getUpdateDate());
  }

  @Test
  void testToDTO_FormalMotivationInnerFieldNull_ReplacedWithDefault() {
    Product product = Product.builder()
      .organizationId("org1")
      .registrationDate(LocalDateTime.now())
      .status(ProductStatus.APPROVED.name())
      .model("M")
      .productGroup("G")
      .category("C")
      .brand("B")
      .capacity("10")
      .formalMotivation(new FormalMotivationDTO(null, null))
      .organizationName("orgName")
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA_ADMIN.getRole());
    assertEquals("", dto.getFormalMotivation().getFormalMotivation(), "Campo null -> default ''");
    assertEquals(LocalDateTime.MIN, dto.getFormalMotivation().getUpdateDate(), "Anche la data va a MIN");
  }

  @Test
  void testToDTO_NoFieldIsNull() {
    Product product = Product.builder()
      .gtinCode("dummy")
      .organizationId(null)
      .registrationDate(null)
      .status(null)
      .model(null)
      .productGroup(null)
      .category(null)
      .brand(null)
      .eprelCode(null)
      .productCode(null)
      .countryOfProduction(null)
      .energyClass(null)
      .productName(null)
      .capacity(null)
      .statusChangeChronology(null)
      .formalMotivation(null)
      .organizationName(null)
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA_ADMIN.getRole());
    assertNotNull(dto);
    assertEquals("", dto.getOrganizationId());
    assertEquals("", dto.getStatus());
    assertEquals("", dto.getModel());
    assertEquals("", dto.getProductGroup());
    assertEquals("", dto.getCategory());
    assertEquals("", dto.getBrand());
    assertEquals("", dto.getEprelCode());
    assertEquals("", dto.getGtinCode());
    assertEquals("", dto.getProductCode());
    assertEquals("", dto.getCountryOfProduction());
    assertEquals("", dto.getEnergyClass());
    assertEquals("", dto.getProductName());
    assertEquals("", dto.getCapacity());
    assertNotNull(dto.getStatusChangeChronology());
    assertNotNull(dto.getFormalMotivation());
    assertEquals("", dto.getFormalMotivation().getFormalMotivation());
    assertEquals(LocalDateTime.MIN, dto.getFormalMotivation().getUpdateDate());
    assertEquals("", dto.getOrganizationName());
  }

  @Test
  void testToDTO_CapacityNA_BecomesEmptyStringInDTO() {
    Product product = Product.builder()
      .organizationId("org1")
      .registrationDate(LocalDateTime.now())
      .status(ProductStatus.APPROVED.name())
      .model("M")
      .productGroup("G")
      .category("C")
      .brand("B")
      .capacity("N\\A")
      .formalMotivation(new FormalMotivationDTO("x", LocalDateTime.now()))
      .organizationName("orgName")
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA_ADMIN.getRole());
    assertEquals("", dto.getCapacity(), "In DTO, 'N\\A' ora diventa stringa vuota");
  }


  // ---------- mapCookingHobToProduct ----------

  @Test
  void testMapCookingHobToProduct() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get("codeProduct")).thenReturn("PROD123");
    when(csvRecord.get("codeGtinEan")).thenReturn("GTIN123");
    when(csvRecord.get("countryOfProduction")).thenReturn("Italy");
    when(csvRecord.get("brand")).thenReturn("BrandX");
    when(csvRecord.get("model")).thenReturn("ModelX");

    Product product = ProductMapper.mapCookingHobToProduct(csvRecord, "org1", "file123", "orgName");
    assertEquals("COOKINGHOBS", product.getCategory());
    assertEquals("N\\A", product.getCapacity());
    assertEquals(ProductStatus.UPLOADED.name(), product.getStatus());
    assertEquals("orgName", product.getOrganizationName());
    assertNotNull(product.getFormalMotivation(), "Default FormalMotivation presente");
  }

  // ---------- mapEprelToProduct ----------

  @Test
  void testMapEprelToProduct_Generic() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get("codeProduct")).thenReturn("PROD123");
    when(csvRecord.get("codeGtinEan")).thenReturn("GTIN123");
    when(csvRecord.get("codeEprel")).thenReturn("EPREL123");
    when(csvRecord.get("countryOfProduction")).thenReturn("Italy");

    EprelProduct eprel = mock(EprelProduct.class);
    when(eprel.getProductGroup()).thenReturn("GroupA");
    when(eprel.getSupplierOrTrademark()).thenReturn("BrandX");
    when(eprel.getModelIdentifier()).thenReturn("ModelX");
    when(eprel.getEnergyClass()).thenReturn("A");

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", "WASHINGMACHINES", "orgName");
    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA_ADMIN.getRole());
    assertEquals("BrandX", dto.getBrand());
    assertEquals("orgName", dto.getOrganizationName());
    assertNotNull(dto.getFormalMotivation());
  }

  @Test
  void testMapEprelToProduct_Ovens_CapacityJoinMultipleCavities() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get("codeProduct")).thenReturn("PROD123");
    when(csvRecord.get("codeGtinEan")).thenReturn("GTIN123");
    when(csvRecord.get("codeEprel")).thenReturn("EPREL123");
    when(csvRecord.get("countryOfProduction")).thenReturn("Italy");

    EprelProduct eprel = new EprelProduct();
    eprel.setProductGroup("OvensGroup");
    eprel.setSupplierOrTrademark("BrandY");
    eprel.setModelIdentifier("ModelOven");
    eprel.setEnergyClass("B");

    EprelProduct.Cavity c1 = new EprelProduct.Cavity();
    c1.setVolume(50);
    EprelProduct.Cavity c2 = new EprelProduct.Cavity();
    c2.setVolume(null);

    eprel.setCavities(List.of(c1, c2));

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", "OVENS", "orgName");
    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA_ADMIN.getRole());
    assertNotNull(dto.getCapacity());
    assertTrue(dto.getCapacity().contains("50 l"), "Prima cavità con valore");
    assertTrue(dto.getCapacity().contains("N\\A"), "Seconda cavità senza volume -> N\\A");
  }

  // --- REFRIGERATINGAPPL: frigorifero vs freezer + VARIABLE_TEMP con subcompartments ---

  @Test
  void testMapEprelToProduct_Refrigerator_CompartmentMatchesRefrigerator() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get("codeProduct")).thenReturn("PROD123");
    when(csvRecord.get("codeGtinEan")).thenReturn("GTIN123");
    when(csvRecord.get("codeEprel")).thenReturn("EPREL123");
    when(csvRecord.get("countryOfProduction")).thenReturn("Italy");

    EprelProduct.RefrigeratorCompartment compartment = new EprelProduct.RefrigeratorCompartment();
    compartment.setCompartmentType("CELLAR");
    compartment.setVolume("5");

    EprelProduct eprel = new EprelProduct();
    eprel.setProductGroup("GroupA");
    eprel.setSupplierOrTrademark("BrandX");
    eprel.setModelIdentifier("ModelX");
    eprel.setEnergyClass("A");
    eprel.setCompartments(List.of(compartment));

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", "REFRIGERATINGAPPL", "orgName");
    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA_ADMIN.getRole());
    assertEquals("BrandX", dto.getBrand());
    assertTrue(dto.getProductName().contains("BrandX"));
    assertTrue(dto.getProductName().contains("ModelX"));
  }

  @Test
  void testMapEprelToProduct_Refrigerator_VariableTempWithRefrigeratorSub() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get("codeProduct")).thenReturn("PROD123");
    when(csvRecord.get("codeGtinEan")).thenReturn("GTIN123");
    when(csvRecord.get("codeEprel")).thenReturn("EPREL123");
    when(csvRecord.get("countryOfProduction")).thenReturn("Italy");

    EprelProduct.SubCompartment sub = new EprelProduct.SubCompartment();
    sub.setCompartmentType("CELLAR");
    EprelProduct.RefrigeratorCompartment compartment = new EprelProduct.RefrigeratorCompartment();
    compartment.setCompartmentType("VARIABLE_TEMP");
    compartment.setSubCompartments(List.of(sub));
    compartment.setVolume("10");

    EprelProduct eprel = new EprelProduct();
    eprel.setProductGroup("GroupA");
    eprel.setSupplierOrTrademark("BrandX");
    eprel.setModelIdentifier("ModelX");
    eprel.setEnergyClass("A");
    eprel.setCompartments(List.of(compartment));

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", "REFRIGERATINGAPPL", "orgName");
    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA_ADMIN.getRole());
    assertEquals("BrandX", dto.getBrand());
    assertNotNull(dto.getProductName());
  }

  @Test
  void testMapEprelToProduct_Refrigerator_VariableTempWithoutRefrigeratorSub() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get("codeProduct")).thenReturn("PROD123");
    when(csvRecord.get("codeGtinEan")).thenReturn("GTIN123");
    when(csvRecord.get("codeEprel")).thenReturn("EPREL123");
    when(csvRecord.get("countryOfProduction")).thenReturn("Italy");

    EprelProduct.SubCompartment sub = new EprelProduct.SubCompartment();
    sub.setCompartmentType("FREEZER");
    EprelProduct.RefrigeratorCompartment compartment = new EprelProduct.RefrigeratorCompartment();
    compartment.setCompartmentType("VARIABLE_TEMP");
    compartment.setSubCompartments(List.of(sub));
    compartment.setVolume("10");

    EprelProduct eprel = new EprelProduct();
    eprel.setProductGroup("GroupA");
    eprel.setSupplierOrTrademark("BrandX");
    eprel.setModelIdentifier("ModelX");
    eprel.setEnergyClass("A");
    eprel.setCompartments(List.of(compartment));

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", "REFRIGERATINGAPPL", "orgName");
    assertEquals("BrandX", product.getBrand());
    assertNotNull(product.getProductName());
  }

  // ---------- mapCapacity ----------

  @ParameterizedTest
  @MethodSource("provideCapacityCases")
  void testMapCapacity(String category, EprelProduct eprel, String expected) {
    String result = ProductMapper.mapCapacity(category, eprel);
    assertEquals(expected, result);
  }

  static Stream<Arguments> provideCapacityCases() {
    // WASHINGMACHINES
    EprelProduct washingMachine = EprelProduct.builder().ratedCapacity("8").build();
    EprelProduct washingMachineNull = EprelProduct.builder().ratedCapacity(null).build();

    EprelProduct tumbleDryer = EprelProduct.builder().ratedCapacity("7").build();
    EprelProduct tumbleDryerNull = EprelProduct.builder().ratedCapacity(null).build();

    EprelProduct washerDrier = EprelProduct.builder().ratedCapacityWash("6").build();
    EprelProduct washerDrierNull = EprelProduct.builder().ratedCapacityWash(null).build();

    EprelProduct.Cavity c1 = new EprelProduct.Cavity();
    c1.setVolume(65);
    EprelProduct oven = new EprelProduct();
    oven.setCavities(List.of(c1));

    EprelProduct ovenNullCavities = new EprelProduct();
    ovenNullCavities.setCavities(null);

    EprelProduct.Cavity nullVolumeCavity = new EprelProduct.Cavity();
    nullVolumeCavity.setVolume(null);
    EprelProduct ovenNullVolume = new EprelProduct();
    ovenNullVolume.setCavities(List.of(nullVolumeCavity));

    EprelProduct dishwasher = EprelProduct.builder().ratedCapacity("12").build();
    EprelProduct dishwasherNull = EprelProduct.builder().ratedCapacity(null).build();

    EprelProduct fridge = EprelProduct.builder().totalVolume("300").build();
    EprelProduct fridgeNull = EprelProduct.builder().totalVolume(null).build();

    EprelProduct unknown = new EprelProduct();

    return Stream.of(
      Arguments.of("WASHINGMACHINES", washingMachine, "8 kg"),
      Arguments.of("WASHINGMACHINES", washingMachineNull, "N\\A"),
      Arguments.of("TUMBLEDRYERS", tumbleDryer, "7 kg"),
      Arguments.of("TUMBLEDRYERS", tumbleDryerNull, "N\\A"),
      Arguments.of("WASHERDRIERS", washerDrier, "6 kg"),
      Arguments.of("WASHERDRIERS", washerDrierNull, "N\\A"),
      Arguments.of("OVENS", oven, "65 l"),
      Arguments.of("OVENS", ovenNullCavities, "N\\A"),
      Arguments.of("OVENS", ovenNullVolume, "N\\A"),
      Arguments.of("DISHWASHERS", dishwasher, "12 c"),
      Arguments.of("DISHWASHERS", dishwasherNull, "N\\A"),
      Arguments.of("REFRIGERATINGAPPL", fridge, "300 l"),
      Arguments.of("REFRIGERATINGAPPL", fridgeNull, "N\\A"),
      Arguments.of("UNKNOWN", unknown, "N\\A"),
      Arguments.of("WASHINGMACHINES", null, "N\\A")
    );
  }

  // ---------- mapProductName ----------

  @Test
  void testMapProductName_CapacityAppendedOnlyWhenNotNA() {
    EprelProduct e = new EprelProduct();
    e.setSupplierOrTrademark("BrandZ");
    e.setModelIdentifier("ModelZ");
    e.setEnergyClass("A");

    String withCapacity = ProductMapper.mapProductName(e, "WASHINGMACHINES", "8 kg");
    assertTrue(withCapacity.endsWith("BrandZ ModelZ 8 kg"),
      "Se la capacity è valorizzata, deve comparire alla fine del nome");

    String withoutCapacity = ProductMapper.mapProductName(e, "WASHINGMACHINES", "N\\A");
    assertTrue(withoutCapacity.endsWith("BrandZ ModelZ"),
      "Se la capacity è 'N\\A', non deve essere appesa");
    assertFalse(withoutCapacity.endsWith("N\\A"), "Non deve chiudersi con 'N\\A'");
  }

  // ---------- mapProductToCsvRow ----------

  @Test
  void testMapProductToCsvRow_CookingHob() {
    Product product = Product.builder()
      .eprelCode("EPREL123")
      .gtinCode("GTIN123")
      .productCode("PROD123")
      .category("COOKINGHOBS")
      .countryOfProduction("Italy")
      .model("ModelX")
      .brand("BrandX")
      .build();

    List<String> headers = List.of("eprelCode", "gtinCode", "productCode", "category", "countryOfProduction", "model", "brand");
    CSVRecord csvRecord = ProductMapper.mapProductToCsvRow(product, "COOKINGHOBS", headers);
    assertNotNull(csvRecord);
    assertEquals("EPREL123", csvRecord.get(0));
    assertEquals("BrandX", csvRecord.get(6));
  }

  @Test
  void testMapProductToCsvRow_OtherCategory() {
    Product product = Product.builder()
      .eprelCode("EPREL123")
      .gtinCode("GTIN123")
      .productCode("PROD123")
      .category("OVENS")
      .countryOfProduction("Italy")
      .build();

    List<String> headers = List.of("eprelCode", "gtinCode", "productCode", "category", "countryOfProduction");
    CSVRecord csvRecord = ProductMapper.mapProductToCsvRow(product, "OVENS", headers);
    assertNotNull(csvRecord);
    assertEquals("OVENS", csvRecord.get(3));
    assertEquals("Italy", csvRecord.get(4));
  }
}
