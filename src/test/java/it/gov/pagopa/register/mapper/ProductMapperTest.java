package it.gov.pagopa.register.mapper;

import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.mapper.operation.ProductMapper;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductMapperTest {

  @Test
  void testToDTO_NullEntity() {
    assertNull(ProductMapper.toDTO(null));
  }

  @Test
  void testToDTO_ValidEntity() {
    Product product = Product.builder()
      .organizationId("org1")
      .registrationDate(LocalDateTime.now())
      .status("APPROVED")
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
      .build();

    ProductDTO dto = ProductMapper.toDTO(product);
    assertNotNull(dto);
    assertEquals("org1", dto.getOrganizationId());
    assertTrue(dto.getProductName().contains("BrandX"));
  }

  @Test
  void testMapCookingHobToProduct() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    when(csvRecord.get("codeProduct")).thenReturn("PROD123");
    when(csvRecord.get("codeGtinEan")).thenReturn("GTIN123");
    when(csvRecord.get("countryOfProduction")).thenReturn("Italy");
    when(csvRecord.get("brand")).thenReturn("BrandX");
    when(csvRecord.get("model")).thenReturn("ModelX");

    Product product = ProductMapper.mapCookingHobToProduct(csvRecord, "org1", "file123");
    assertEquals("COOKINGHOBS", product.getCategory());
    assertEquals("N\\A", product.getCapacity());
  }

  @Test
  void testMapEprelToProduct() {
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

    Product product = ProductMapper.mapEprelToProduct(csvRecord, eprel, "org1", "file123", "WASHINGMACHINES");
    assertEquals("BrandX", product.getBrand());
  }

  @Test
  void testMapCapacity_NullEprel() {
    assertEquals("N\\A", ProductMapper.mapCapacity("WASHINGMACHINES", null));
  }

  @Test
  void testMapCapacity_WashingMachines() {
    EprelProduct eprel = mock(EprelProduct.class);
    when(eprel.getRatedCapacity()).thenReturn("8");
    assertEquals("8 kg", ProductMapper.mapCapacity("WASHINGMACHINES", eprel));
  }

  @Test
  void testMapCapacity_WashingMachines_WithCapacity() {
    EprelProduct eprel = new EprelProduct();
    eprel.setRatedCapacity("8");
    assertEquals("8 kg", ProductMapper.mapCapacity("WASHINGMACHINES", eprel));
  }

  @Test
  void testMapCapacity_TumbleDryers_WithCapacity() {
    EprelProduct eprel = new EprelProduct();
    eprel.setRatedCapacity("7");
    assertEquals("7 kg", ProductMapper.mapCapacity("TUMBLEDRYERS", eprel));
  }

  @Test
  void testMapCapacity_WasherDriers_WithCapacity() {
    EprelProduct eprel = new EprelProduct();
    eprel.setRatedCapacityWash("6");
    assertEquals("6 kg", ProductMapper.mapCapacity("WASHERDRIERS", eprel));
  }

  @Test
  void testMapCapacity_Ovens_WithCavityVolume() {
    EprelProduct eprel = new EprelProduct();
    EprelProduct.Cavities cavities = new EprelProduct.Cavities();
    cavities.setVolume("65");
    eprel.setCavities(cavities);
    assertEquals("65 l", ProductMapper.mapCapacity("OVENS", eprel));
  }

  @Test
  void testMapCapacity_Dishwashers_WithCapacity() {
    EprelProduct eprel = new EprelProduct();
    eprel.setRatedCapacity("12");
    assertEquals("12 c", ProductMapper.mapCapacity("DISHWASHERS", eprel));
  }

  @Test
  void testMapCapacity_RefrigeratingAppl_WithVolume() {
    EprelProduct eprel = new EprelProduct();
    eprel.setTotalVolume("300");
    assertEquals("300 l", ProductMapper.mapCapacity("REFRIGERATINGAPPL", eprel));
  }

  @Test
  void testMapCapacity_UnknownCategory() {
    EprelProduct eprel = new EprelProduct();
    assertEquals("N\\A", ProductMapper.mapCapacity("UNKNOWN", eprel));
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
