package it.gov.pagopa.register.mapper;

import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.mapper.operation.ProductMapper;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.StatusChangeEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.register.utils.ObjectMaker.buildStatusChangeEventsList;
import static org.junit.jupiter.api.Assertions.*;

class ProductMapperTest {

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
  void testToDTO_RoleInvitalia_StatusSupervised_Unchanged_ChronologyVisible() {
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
      .productName("CategoryA BrandX ModelX 10")
      .fullProductName("GTIN123 - CategoryA BrandX ModelX 10")
      .statusChangeChronology(buildStatusChangeEventsList())
      .formalMotivation("OK")
      .organizationName("orgName")
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA.getRole());

    assertNotNull(dto);
    assertEquals(ProductStatus.SUPERVISED.name(), dto.getStatus());

    assertNotNull(dto.getStatusChangeChronology());
    assertFalse(dto.getStatusChangeChronology().isEmpty());
    StatusChangeEvent first = dto.getStatusChangeChronology().get(0);
    assertNotEquals("-", first.getUsername());

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
    assertNotNull(dto.getStatusChangeChronology());
    assertEquals(0, dto.getStatusChangeChronology().size());
    assertEquals("full-name", dto.getFullProductName());
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
    assertEquals("", dto.getCapacity());
    assertEquals("full-name", dto.getFullProductName());
  }

  @Test
  void testParseScart() {
    assertTrue(ProductMapper.parseScart("SI"));
    assertTrue(ProductMapper.parseScart("si"));
    assertTrue(ProductMapper.parseScart("  SI  "));
    assertTrue(ProductMapper.parseScart("TRUE"));
    assertTrue(ProductMapper.parseScart("True"));
    assertTrue(ProductMapper.parseScart("1"));
    assertFalse(ProductMapper.parseScart("NO"));
    assertFalse(ProductMapper.parseScart("no"));
    assertFalse(ProductMapper.parseScart("FALSE"));
    assertFalse(ProductMapper.parseScart("False"));
    assertFalse(ProductMapper.parseScart("0"));
    assertFalse(ProductMapper.parseScart("INVALID"));
    assertFalse(ProductMapper.parseScart(""));
    assertFalse(ProductMapper.parseScart("   "));
    assertFalse(ProductMapper.parseScart(null));
  }

  @Test
  void testNormalizeConnectivity() {
    assertEquals("LAN e WI-FI", ProductMapper.normalizeConnectivity("  LAN   e   WI-FI  "));
    String longString = "A".repeat(150);
    assertEquals(100, ProductMapper.normalizeConnectivity(longString).length());
    assertNull(ProductMapper.normalizeConnectivity(null));
    assertNull(ProductMapper.normalizeConnectivity("   "));
  }

  @Test
  void testNormalizeTechnicalSheetUrl() {
    assertEquals("https://example.com/sheet", ProductMapper.normalizeTechnicalSheetUrl("  https://example.com/  sheet  "));
    String longUrl = "https://example.com/" + "a".repeat(300);
    assertEquals(255, ProductMapper.normalizeTechnicalSheetUrl(longUrl).length());
    assertNull(ProductMapper.normalizeTechnicalSheetUrl(null));
    assertNull(ProductMapper.normalizeTechnicalSheetUrl("   "));
  }

  @Test
  void testToDTO_NewFieldsMapped() {
    Product product = Product.builder()
      .organizationId("org1")
      .registrationDate(LocalDateTime.now())
      .status(ProductStatus.APPROVED.name())
      .category("C")
      .productName("name")
      .fullProductName("full-name")
      .organizationName("orgName")
      .scart(true)
      .connectivity("LAN")
      .technicalSheetUrl("https://url.it")
      .build();

    ProductDTO dto = ProductMapper.toDTO(product, UserRole.INVITALIA.getRole());

    assertNotNull(dto);
    assertTrue(dto.getScart());
    assertEquals("LAN", dto.getConnectivity());
    assertEquals("https://url.it", dto.getTechnicalSheetUrl());
  }

}
