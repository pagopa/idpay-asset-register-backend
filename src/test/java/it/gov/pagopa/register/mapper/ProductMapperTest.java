package it.gov.pagopa.register.mapper;

import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.mapper.operation.ProductMapper;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.StatusChangeEvent;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.utils.ObjectMaker.buildStatusChangeEventsList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductMapperTest {

  // ---------- toDTO ----------

  @Test
  void testToDTO_NullEntity_ReturnsNull() {
    assertNull(ProductMapper.toDTO(null, UserRole.INVITALIA.getRole()));
  }

  @Test
  void testToDTO_RoleOperatore_StatusSupervised_Downgraded_ChronologyMasked() {
    List<StatusChangeEvent> original = buildStatusChangeEventsList();

    Product product = Product.builder()
      .organizationId("org1")
      .registrationDate(LocalDateTime.of(2025, 10, 3, 18, 53, 24))
      .status(ProductStatus.SUPERVISED.name())
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
      .statusChangeChronology(new ArrayList<>(original))
      .productName("CategoryA BrandX ModelX 10")
      .fullProductName("GTIN123 - CategoryA BrandX ModelX 10")
      .formalMotivation("OK")
      .organizationName("orgName")
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.OPERATORE.getRole());

    assertNotNull(dto);
    assertEquals(ProductStatus.UPLOADED.name(), dto.getStatus(), "Con ruolo OPERATORE e stato SUPERVISED deve diventare UPLOADED");

    assertNotNull(dto.getStatusChangeChronology());
    assertEquals(original.size(), dto.getStatusChangeChronology().size());
    for (int i = 0; i < original.size(); i++) {
      StatusChangeEvent src = original.get(i);
      StatusChangeEvent masked = dto.getStatusChangeChronology().get(i);
      assertEquals("-", masked.getUsername());
      assertEquals("-", masked.getRole());
      assertEquals("-", masked.getMotivation());
      assertEquals(src.getUpdateDate(), masked.getUpdateDate());
      assertEquals(src.getCurrentStatus(), masked.getCurrentStatus());
      assertEquals(src.getTargetStatus(), masked.getTargetStatus());
    }

    assertEquals("GTIN123 - CategoryA BrandX ModelX 10", dto.getFullProductName());
    assertEquals("10", dto.getCapacity());
  }

  @Test
  void testToDTO_RoleInvitalia_StatusSupervised_Unchanged_ChronologyVisible() {
    Product product = Product.builder()
      .organizationId("org1")
      .registrationDate(LocalDateTime.of(2025, 10, 3, 18, 53, 24))
      .status(ProductStatus.SUPERVISED.name()) // << nuovo caso
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
      .fullProductName("GTIN123 - CategoryA BrandX ModelX 10")
      .statusChangeChronology(buildStatusChangeEventsList())
      .formalMotivation("OK")
      .organizationName("orgName")
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA.getRole());

    assertNotNull(dto);
    assertEquals(ProductStatus.SUPERVISED.name(), dto.getStatus(), "Con ruolo non OPERATORE lo stato SUPERVISED deve restare invariato");

    assertNotNull(dto.getStatusChangeChronology());
    assertFalse(dto.getStatusChangeChronology().isEmpty(), "La chronology deve essere presente per ruoli non OPERATORE");
    StatusChangeEvent first = dto.getStatusChangeChronology().get(0);
    assertNotEquals("-", first.getUsername(), "Per ruoli non OPERATORE la chronology non deve essere mascherata");

    assertEquals("OK", dto.getFormalMotivation());
    assertEquals("GTIN123 - CategoryA BrandX ModelX 10", dto.getFullProductName());
  }


  @Test
  void testToDTO_ChronologyNull_ReturnsEmptyList() {
    Product product = Product.builder()
      .organizationId("org1")
      .registrationDate(LocalDateTime.now())
      .status(ProductStatus.APPROVED.name())
      .category("C")
      .productName("name")
      .fullProductName("full-name")
      .organizationName("orgName")
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA.getRole());

    assertNotNull(dto);
    assertNotNull(dto.getStatusChangeChronology(), "La chronology non deve essere null");
    assertEquals(0, dto.getStatusChangeChronology().size(), "Se chronology è null sull'entità, deve diventare lista vuota");
    assertEquals("full-name", dto.getFullProductName(), "fullProductName deve propagare nel DTO");
  }

  @Test
  void testToDTO_RoleOperatore_StatusDowngraded_ChronologyMasked_FullNamePreserved() {
    List<StatusChangeEvent> original = buildStatusChangeEventsList();

    Product product = Product.builder()
      .organizationId("org1")
      .registrationDate(LocalDateTime.of(2025, 10, 3, 18, 53, 24))
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
      .statusChangeChronology(new ArrayList<>(original))
      .productName("CategoryA BrandX ModelX 10")
      .fullProductName("GTIN123 - CategoryA BrandX ModelX 10")
      .formalMotivation("OK")
      .organizationName("orgName")
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.OPERATORE.getRole());

    assertNotNull(dto);
    assertEquals(ProductStatus.UPLOADED.name(), dto.getStatus());
    assertNotNull(dto.getStatusChangeChronology());
    assertEquals(original.size(), dto.getStatusChangeChronology().size());

    for (int i = 0; i < original.size(); i++) {
      StatusChangeEvent src = original.get(i);
      StatusChangeEvent masked = dto.getStatusChangeChronology().get(i);
      assertEquals("-", masked.getUsername());
      assertEquals("-", masked.getRole());
      assertEquals("-", masked.getMotivation());
      assertEquals(src.getUpdateDate(), masked.getUpdateDate());
      assertEquals(src.getCurrentStatus(), masked.getCurrentStatus());
      assertEquals(src.getTargetStatus(), masked.getTargetStatus());
    }

    assertEquals("GTIN123 - CategoryA BrandX ModelX 10", dto.getFullProductName());
    assertEquals("10", dto.getCapacity());
  }

  @Test
  void testToDTO_RoleInvitalia_StatusUnchanged_ChronologyVisible_FullNameVisible() {
    Product product = Product.builder()
      .organizationId("org1")
      .registrationDate(LocalDateTime.of(2025, 10, 3, 18, 53, 24))
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
      .fullProductName("GTIN123 - CategoryA BrandX ModelX 10")
      .statusChangeChronology(buildStatusChangeEventsList())
      .formalMotivation("OK")
      .organizationName("orgName")
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA.getRole());

    assertEquals(ProductStatus.WAIT_APPROVED.name(), dto.getStatus());
    assertNotNull(dto.getStatusChangeChronology());
    assertEquals("OK", dto.getFormalMotivation());
    assertEquals("GTIN123 - CategoryA BrandX ModelX 10", dto.getFullProductName());
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
      .productName("name")
      .fullProductName("full-name")
      .formalMotivation("OK")
      .organizationName("orgName")
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA_ADMIN.getRole());
    assertEquals("", dto.getCapacity(), "In DTO, 'N\\A' diventa stringa vuota");
    assertEquals("full-name", dto.getFullProductName());
  }

  // ---------- mapCookingHobToProduct ----------

  @Test
  void testMapCookingHobToProduct_FullProductNameContainsGtinAndBaseName() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get(CODE_PRODUCT)).thenReturn("PROD123");
    when(csvRecord.get(CODE_GTIN_EAN)).thenReturn("GTIN123");
    when(csvRecord.get(COUNTRY_OF_PRODUCTION)).thenReturn("Italy");
    when(csvRecord.get(BRAND)).thenReturn("BrandX");
    when(csvRecord.get(MODEL)).thenReturn("ModelX");

    Product product = ProductMapper.mapCookingHobToProduct(csvRecord, "org1", "file123", "orgName");

    assertEquals(COOKINGHOBS, product.getCategory());
    assertEquals("N\\A", product.getCapacity());
    assertEquals(ProductStatus.UPLOADED.name(), product.getStatus());
    assertEquals("orgName", product.getOrganizationName());
    assertNotNull(product.getFormalMotivation(), "Default FormalMotivation presente");

    assertNotNull(product.getProductName());
    assertNotNull(product.getFullProductName(), "FullProductName deve essere valorizzato");

    String expectedFull = "GTIN123 - " + product.getProductName();
    assertEquals(expectedFull, product.getFullProductName(), "fullProductName deve combaciare esattamente");

    assertTrue(product.getFullProductName().contains("BrandX"));
    assertTrue(product.getFullProductName().contains("ModelX"));
  }

  // ---------- mapEprelToProduct ----------

  @Test
  void testMapEprelToProduct_Generic_IncludesFullProductNameWithCapacity() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get(CODE_PRODUCT)).thenReturn("PROD123");
    when(csvRecord.get(CODE_GTIN_EAN)).thenReturn("GTIN999");
    when(csvRecord.get(CODE_EPREL)).thenReturn("EPREL123");
    when(csvRecord.get(COUNTRY_OF_PRODUCTION)).thenReturn("Italy");

    EprelProduct eprel = new EprelProduct();
    eprel.setProductGroup("GroupA");
    eprel.setSupplierOrTrademark("BrandX");
    eprel.setModelIdentifier("ModelX");
    eprel.setEnergyClass("A");
    eprel.setRatedCapacity("8");

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", WASHINGMACHINES, "orgName");

    assertEquals("BrandX", product.getBrand());
    assertEquals("orgName", product.getOrganizationName());
    assertNotNull(product.getFormalMotivation());
    assertEquals("8 kg", product.getCapacity());

    assertNotNull(product.getProductName());
    assertTrue(product.getProductName().endsWith("8 kg"));

    String expectedFull = ProductMapper.mapName("GTIN999", eprel, WASHINGMACHINES, "8 kg");
    assertEquals(expectedFull, product.getFullProductName(), "fullProductName (EPREL) deve combaciare esattamente");

    assertTrue(product.getFullProductName().contains("BrandX ModelX 8 kg"));
  }

  @Test
  void testMapEprelToProduct_Ovens_CapacityJoinMultipleCavities_FullNameAligned() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get(CODE_PRODUCT)).thenReturn("PROD123");
    when(csvRecord.get(CODE_GTIN_EAN)).thenReturn("GTIN777");
    when(csvRecord.get(CODE_EPREL)).thenReturn("EPREL123");
    when(csvRecord.get(COUNTRY_OF_PRODUCTION)).thenReturn("Italy");

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

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", OVENS, "orgName");

    assertNotNull(product.getCapacity());
    assertTrue(product.getCapacity().contains("50 l"));
    assertTrue(product.getCapacity().contains("N\\A"));

    assertNotNull(product.getFullProductName());
    assertTrue(product.getFullProductName().contains("BrandY ModelOven"));
    assertTrue(product.getFullProductName().contains("50 l"));
  }

  // --- REFRIGERATINGAPPL: frigorifero vs freezer + VARIABLE_TEMP con subcompartments ---

  @Test
  void testMapEprelToProduct_Refrigerator_CompartmentMatchesRefrigerator_FullName() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get(CODE_PRODUCT)).thenReturn("PROD123");
    when(csvRecord.get(CODE_GTIN_EAN)).thenReturn("GTIN555");
    when(csvRecord.get(CODE_EPREL)).thenReturn("EPREL123");
    when(csvRecord.get(COUNTRY_OF_PRODUCTION)).thenReturn("Italy");

    EprelProduct.RefrigeratorCompartment compartment = new EprelProduct.RefrigeratorCompartment();
    compartment.setCompartmentType("CELLAR");
    compartment.setVolume("5");

    EprelProduct eprel = new EprelProduct();
    eprel.setProductGroup("GroupA");
    eprel.setSupplierOrTrademark("BrandX");
    eprel.setModelIdentifier("ModelX");
    eprel.setEnergyClass("A");
    eprel.setCompartments(List.of(compartment));
    eprel.setTotalVolume("300");

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", REFRIGERATINGAPPL, "orgName");

    assertNotNull(product.getProductName());
    assertTrue(product.getProductName().contains("BrandX"));
    assertTrue(product.getProductName().contains("ModelX"));

    String expectedFull = ProductMapper.mapName(
      "GTIN555", eprel, REFRIGERATINGAPPL,
      ProductMapper.mapCapacity(REFRIGERATINGAPPL, eprel)
    );
    assertEquals(expectedFull, product.getFullProductName(), "fullProductName (fridge) deve combaciare esattamente");
    assertTrue(product.getFullProductName().contains("BrandX ModelX"));
  }

  @Test
  void testMapEprelToProduct_Refrigerator_VariableTempWithRefrigeratorSub_FullNamePresent() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get(CODE_PRODUCT)).thenReturn("PROD123");
    when(csvRecord.get(CODE_GTIN_EAN)).thenReturn("GTIN444");
    when(csvRecord.get(CODE_EPREL)).thenReturn("EPREL123");
    when(csvRecord.get(COUNTRY_OF_PRODUCTION)).thenReturn("Italy");

    EprelProduct.SubCompartment sub = new EprelProduct.SubCompartment();
    sub.setCompartmentType("CELLAR");
    EprelProduct.RefrigeratorCompartment compartment = new EprelProduct.RefrigeratorCompartment();
    compartment.setCompartmentType(VARIABLE_TEMP);
    compartment.setSubCompartments(List.of(sub));
    compartment.setVolume("10");

    EprelProduct eprel = new EprelProduct();
    eprel.setProductGroup("GroupA");
    eprel.setSupplierOrTrademark("BrandX");
    eprel.setModelIdentifier("ModelX");
    eprel.setEnergyClass("A");
    eprel.setCompartments(List.of(compartment));
    eprel.setTotalVolume("250");

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", REFRIGERATINGAPPL, "orgName");

    assertNotNull(product.getProductName());
    assertNotNull(product.getFullProductName());

    String expectedFull = ProductMapper.mapName(
      "GTIN444", eprel, REFRIGERATINGAPPL,
      ProductMapper.mapCapacity(REFRIGERATINGAPPL, eprel)
    );
    assertEquals(expectedFull, product.getFullProductName(), "fullProductName (var temp con sub frigo) deve combaciare esattamente");
  }

  @Test
  void testMapEprelToProduct_Refrigerator_VariableTempWithoutRefrigeratorSub_FullNamePresent() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get(CODE_PRODUCT)).thenReturn("PROD123");
    when(csvRecord.get(CODE_GTIN_EAN)).thenReturn("GTIN333");
    when(csvRecord.get(CODE_EPREL)).thenReturn("EPREL123");
    when(csvRecord.get(COUNTRY_OF_PRODUCTION)).thenReturn("Italy");

    EprelProduct.SubCompartment sub = new EprelProduct.SubCompartment();
    sub.setCompartmentType("FREEZER");
    EprelProduct.RefrigeratorCompartment compartment = new EprelProduct.RefrigeratorCompartment();
    compartment.setCompartmentType(VARIABLE_TEMP);
    compartment.setSubCompartments(List.of(sub));
    compartment.setVolume("10");

    EprelProduct eprel = new EprelProduct();
    eprel.setProductGroup("GroupA");
    eprel.setSupplierOrTrademark("BrandX");
    eprel.setModelIdentifier("ModelX");
    eprel.setEnergyClass("A");
    eprel.setCompartments(List.of(compartment));
    eprel.setTotalVolume("200");

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", REFRIGERATINGAPPL, "orgName");

    assertNotNull(product.getProductName());
    assertNotNull(product.getFullProductName());

    String expectedFull = ProductMapper.mapName(
      "GTIN333", eprel, REFRIGERATINGAPPL,
      ProductMapper.mapCapacity(REFRIGERATINGAPPL, eprel)
    );
    assertEquals(expectedFull, product.getFullProductName(), "fullProductName (var temp senza sub frigo) deve combaciare esattamente");
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
    EprelProduct washingMachine = new EprelProduct();
    washingMachine.setRatedCapacity("8");
    EprelProduct washingMachineNull = new EprelProduct();
    washingMachineNull.setRatedCapacity(null);

    // TUMBLEDRYERS
    EprelProduct tumbleDryer = new EprelProduct();
    tumbleDryer.setRatedCapacity("7");
    EprelProduct tumbleDryerNull = new EprelProduct();
    tumbleDryerNull.setRatedCapacity(null);

    // WASHERDRIERS
    EprelProduct washerDrier = new EprelProduct();
    washerDrier.setRatedCapacityWash("6");
    EprelProduct washerDrierNull = new EprelProduct();
    washerDrierNull.setRatedCapacityWash(null);

    // OVENS
    EprelProduct oven = new EprelProduct();
    EprelProduct.Cavity cavity1 = new EprelProduct.Cavity();
    cavity1.setVolume(65);
    oven.setCavities(List.of(cavity1));

    EprelProduct ovenNullCavities = new EprelProduct();
    ovenNullCavities.setCavities(null);

    EprelProduct ovenNullVolume = new EprelProduct();
    EprelProduct.Cavity nullVolumeCavity = new EprelProduct.Cavity();
    ovenNullVolume.setCavities(List.of(nullVolumeCavity));

    // DISHWASHERS
    EprelProduct dishwasher = new EprelProduct();
    dishwasher.setRatedCapacity("12");
    EprelProduct dishwasherNull = new EprelProduct();
    dishwasherNull.setRatedCapacity(null);

    // REFRIGERATINGAPPL
    EprelProduct fridge = new EprelProduct();
    fridge.setTotalVolume("300");
    EprelProduct fridgeNull = new EprelProduct();
    fridgeNull.setTotalVolume(null);

    // UNKNOWN + null
    EprelProduct unknown = new EprelProduct();

    return Stream.of(
      Arguments.of(WASHINGMACHINES, washingMachine, "8 kg"),
      Arguments.of(WASHINGMACHINES, washingMachineNull, "N\\A"),
      Arguments.of(TUMBLEDRYERS, tumbleDryer, "7 kg"),
      Arguments.of(TUMBLEDRYERS, tumbleDryerNull, "N\\A"),
      Arguments.of(WASHERDRIERS, washerDrier, "6 kg"),
      Arguments.of(WASHERDRIERS, washerDrierNull, "N\\A"),
      Arguments.of(OVENS, oven, "65 l"),
      Arguments.of(OVENS, ovenNullCavities, "N\\A"),
      Arguments.of(OVENS, ovenNullVolume, "N\\A"),
      Arguments.of(DISHWASHERS, dishwasher, "12 c"),
      Arguments.of(DISHWASHERS, dishwasherNull, "N\\A"),
      Arguments.of(REFRIGERATINGAPPL, fridge, "300 l"),
      Arguments.of(REFRIGERATINGAPPL, fridgeNull, "N\\A"),
      Arguments.of("UNKNOWN", unknown, "N\\A"),
      Arguments.of(WASHINGMACHINES, null, "N\\A")
    );
  }

  // ---------- mapProductName & mapFullProductName ----------

  @Test
  void testMapProductName_CapacityAppendedOnlyWhenNotNA() {
    EprelProduct e = new EprelProduct();
    e.setSupplierOrTrademark("BrandZ");
    e.setModelIdentifier("ModelZ");
    e.setEnergyClass("A");

    String withCapacity = ProductMapper.mapName(null, e, WASHINGMACHINES, "8 kg");
    assertTrue(withCapacity.endsWith("BrandZ ModelZ 8 kg"),
      "Se la capacity è valorizzata, deve comparire alla fine del nome");

    String withoutCapacity = ProductMapper.mapName(null, e, WASHINGMACHINES, "N\\A");
    assertTrue(withoutCapacity.endsWith("BrandZ ModelZ"),
      "Se la capacity è 'N\\A', non deve essere appesa");
    assertFalse(withoutCapacity.endsWith("N\\A"), "Non deve chiudersi con 'N\\A'");
  }

  @Test
  void testMapFullProductName_CapacityAppendedOnlyWhenNotNA_AndStartsWithGTIN() {
    EprelProduct e = new EprelProduct();
    e.setSupplierOrTrademark("BrandZ");
    e.setModelIdentifier("ModelZ");
    e.setEnergyClass("A");

    String withCapacity = ProductMapper.mapName("GTIN111", e, WASHINGMACHINES, "8 kg");
    assertEquals(withCapacity, ProductMapper.mapName("GTIN111", e, WASHINGMACHINES, "8 kg"),
      "Il fullProductName deve essere costruito esattamente");
    assertTrue(withCapacity.endsWith("BrandZ ModelZ 8 kg"),
      "Se la capacity è valorizzata, deve comparire alla fine del nome");

    String withoutCapacity = ProductMapper.mapName("GTIN111", e, WASHINGMACHINES, "N\\A");
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
      .category(COOKINGHOBS)
      .countryOfProduction("Italy")
      .model("ModelX")
      .brand("BrandX")
      .build();

    List<String> headers = List.of("eprelCode", "gtinCode", "productCode", "category", "countryOfProduction", "model", "brand");
    CSVRecord csvRecord = ProductMapper.mapProductToCsvRow(product, COOKINGHOBS, headers);

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
      .category(OVENS)
      .countryOfProduction("Italy")
      .build();

    List<String> headers = List.of("eprelCode", "gtinCode", "productCode", "category", "countryOfProduction");
    CSVRecord csvRecord = ProductMapper.mapProductToCsvRow(product, OVENS, headers);

    assertNotNull(csvRecord);
    assertEquals(OVENS, csvRecord.get(3));
    assertEquals("Italy", csvRecord.get(4));
  }
}
