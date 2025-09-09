package it.gov.pagopa.register.mapper;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductMapperTest {

  @Test
  void testToDTO_NullEntity() {
    assertNull(ProductMapper.toDTO(null, null));
  }

  @Test
  void testToDTO_RoleOperatore() {
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
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.OPERATORE.getRole());
    assertNotNull(dto);
    assertEquals(ProductStatus.UPLOADED.name(), dto.getStatus());
    assertNull(dto.getStatusChangeChronology());

  }

  @Test
  void testToDTO_RoleInvitalia() {
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
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA_ADMIN.getRole());
    assertEquals(ProductStatus.WAIT_APPROVED.name(), dto.getStatus());
    assertNotNull(dto.getStatusChangeChronology());

  }

  @Test
  void testMapCookingHobToProduct() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get("codeProduct")).thenReturn("PROD123");
    when(csvRecord.get("codeGtinEan")).thenReturn("GTIN123");
    when(csvRecord.get("countryOfProduction")).thenReturn("Italy");
    when(csvRecord.get("brand")).thenReturn("BrandX");
    when(csvRecord.get("model")).thenReturn("ModelX");

    Product product = ProductMapper.mapCookingHobToProduct(csvRecord, "org1", "file123","orgName");
    assertEquals("COOKINGHOBS", product.getCategory());
    assertEquals("N\\A", product.getCapacity());
  }

  @Test
  void testMapEprelToProductGeneric() {
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

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", "WASHINGMACHINES","orgName");
    assertEquals("BrandX", product.getBrand());
  }

  //Compartment con uno dei tipi refrigerator
  @Test
  void testMapEprelToProductRefrigerator() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get("codeProduct")).thenReturn("PROD123");
    when(csvRecord.get("codeGtinEan")).thenReturn("GTIN123");
    when(csvRecord.get("codeEprel")).thenReturn("EPREL123");
    when(csvRecord.get("countryOfProduction")).thenReturn("Italy");

    EprelProduct.RefrigeratorCompartment compartment = new EprelProduct.RefrigeratorCompartment();
    compartment.setCompartmentType("CELLAR");
    compartment.setVolume("5");

    List<EprelProduct.RefrigeratorCompartment> compartmentList = List.of(compartment);

    EprelProduct eprel = mock(EprelProduct.class);
    when(eprel.getProductGroup()).thenReturn("GroupA");
    when(eprel.getSupplierOrTrademark()).thenReturn("BrandX");
    when(eprel.getModelIdentifier()).thenReturn("ModelX");
    when(eprel.getEnergyClass()).thenReturn("A");
    when(eprel.getCompartments()).thenReturn(compartmentList);

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", "REFRIGERATINGAPPL","orgName");
    assertEquals("BrandX", product.getBrand());
  }

  //Compartment senza uno dei tipi refrigerator
  @Test
  void testMapEprelToProductFreezer() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get("codeProduct")).thenReturn("PROD123");
    when(csvRecord.get("codeGtinEan")).thenReturn("GTIN123");
    when(csvRecord.get("codeEprel")).thenReturn("EPREL123");
    when(csvRecord.get("countryOfProduction")).thenReturn("Italy");

    EprelProduct.RefrigeratorCompartment compartment = new EprelProduct.RefrigeratorCompartment();
    compartment.setCompartmentType("FREEZER");
    compartment.setVolume("5");

    List<EprelProduct.RefrigeratorCompartment> compartmentList = List.of(compartment);

    EprelProduct eprel = mock(EprelProduct.class);
    when(eprel.getProductGroup()).thenReturn("GroupA");
    when(eprel.getSupplierOrTrademark()).thenReturn("BrandX");
    when(eprel.getModelIdentifier()).thenReturn("ModelX");
    when(eprel.getEnergyClass()).thenReturn("A");
    when(eprel.getCompartments()).thenReturn(compartmentList);

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", "REFRIGERATINGAPPL","orgName");
    assertEquals("BrandX", product.getBrand());
  }

  //Compartment VARIABLE_TEMP con subcompartment di uno dei tipi refrigerator
  @Test
  void testMapEprelToProductSubRefrigerator() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get("codeProduct")).thenReturn("PROD123");
    when(csvRecord.get("codeGtinEan")).thenReturn("GTIN123");
    when(csvRecord.get("codeEprel")).thenReturn("EPREL123");
    when(csvRecord.get("countryOfProduction")).thenReturn("Italy");

    EprelProduct.RefrigeratorCompartment compartment = new EprelProduct.RefrigeratorCompartment();
    compartment.setCompartmentType("VARIABLE_TEMP");
    compartment.setVolume("5");

    List<EprelProduct.RefrigeratorCompartment> compartmentList = List.of(compartment);

    EprelProduct.SubCompartment subCompartment = new EprelProduct.SubCompartment();
    subCompartment.setSubCompartmentType("CELLAR");

    List<EprelProduct.SubCompartment> subCompartmentList = List.of(subCompartment);
    compartment.setSubCompartments(subCompartmentList);

    EprelProduct eprel = mock(EprelProduct.class);
    when(eprel.getProductGroup()).thenReturn("GroupA");
    when(eprel.getSupplierOrTrademark()).thenReturn("BrandX");
    when(eprel.getModelIdentifier()).thenReturn("ModelX");
    when(eprel.getEnergyClass()).thenReturn("A");
    when(eprel.getCompartments()).thenReturn(compartmentList);

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", "REFRIGERATINGAPPL","orgName");
    assertEquals("BrandX", product.getBrand());
  }

  //Compartment VARIABLE_TEMP senza subcompartment di uno dei tipi refrigerator
  @Test
  void testMapEprelToProductSubFreezer() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get("codeProduct")).thenReturn("PROD123");
    when(csvRecord.get("codeGtinEan")).thenReturn("GTIN123");
    when(csvRecord.get("codeEprel")).thenReturn("EPREL123");
    when(csvRecord.get("countryOfProduction")).thenReturn("Italy");

    EprelProduct.RefrigeratorCompartment compartment = new EprelProduct.RefrigeratorCompartment();
    compartment.setCompartmentType("VARIABLE_TEMP");
    compartment.setVolume("5");

    List<EprelProduct.RefrigeratorCompartment> compartmentList = List.of(compartment);

    EprelProduct.SubCompartment subCompartment = new EprelProduct.SubCompartment();
    subCompartment.setSubCompartmentType("FREEZER");

    List<EprelProduct.SubCompartment> subCompartmentList = List.of(subCompartment);
    compartment.setSubCompartments(subCompartmentList);

    EprelProduct eprel = mock(EprelProduct.class);
    when(eprel.getProductGroup()).thenReturn("GroupA");
    when(eprel.getSupplierOrTrademark()).thenReturn("BrandX");
    when(eprel.getModelIdentifier()).thenReturn("ModelX");
    when(eprel.getEnergyClass()).thenReturn("A");
    when(eprel.getCompartments()).thenReturn(compartmentList);

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", "REFRIGERATINGAPPL","orgName");
    assertEquals("BrandX", product.getBrand());
  }


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

    // UNKNOWN
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
  }
}
