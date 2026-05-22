package it.gov.pagopa.register.mapper;

import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.mapper.operation.ProductMapper;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.StatusChangeEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.WASHINGMACHINES;
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

}
