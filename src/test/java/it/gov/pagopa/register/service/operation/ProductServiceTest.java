package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.configuration.InitiativeConfigMap;
import it.gov.pagopa.register.connector.notification.NotificationService;
import it.gov.pagopa.register.dto.operation.*;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.UpdateError.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock private ProductRepository productRepository;
  @Mock private NotificationService notificationService;
  @Mock private InitiativeConfigMap initiativeConfigMap;

  @InjectMocks private ProductService productService;

  private static final String ORG_ID = "org123";
  private static final String USERNAME = "user";

  // ---------------- fetchProductsByFilters ----------------

  @Test
  void fetchProductsByFilters_happyPath() {
    Pageable pageable = PageRequest.of(0, 2);

    Product p1 = Product.builder()
      .organizationId(ORG_ID).status(ProductStatus.UPLOADED.name())
      .brand("B1").model("M1").category("C1")
      .productName("C1 B1 M1")
      .fullProductName("PO01 - C1 B1 M1")
      .registrationDate(LocalDateTime.now())
      .build();

    Product p2 = Product.builder()
      .organizationId(ORG_ID).status(ProductStatus.UPLOADED.name())
      .brand("B2").model("M2").category("C2")
      .productName("C2 B2 M2")
      .fullProductName("PO02 - C1 B1 M1")
      .registrationDate(LocalDateTime.now())
      .build();

    when(productRepository.getCriteria(any(ProductCriteriaDTO.class))).thenReturn(new Criteria());
    when(productRepository.findByFilter(any(Criteria.class), eq(pageable))).thenReturn(List.of(p1, p2));
    when(productRepository.getCount(any(Criteria.class))).thenReturn(2L);

    ProductListDTO dto = productService.fetchProductsByFilters(
      ORG_ID, null, null, null, null, null, null,null, null, null, null, ProductStatus.UPLOADED.name(),
      pageable, UserRole.INVITALIA.getRole()
    );

    assertNotNull(dto);
    assertEquals(2, dto.getContent().size());
    assertEquals(0, dto.getPageNo());
    assertEquals(2, dto.getPageSize());
    assertEquals(2, dto.getTotalElements());
    assertEquals(1, dto.getTotalPages());
    assertEquals("C1 B1 M1", dto.getContent().get(0).getProductName());
    assertEquals("PO01 - C1 B1 M1", dto.getContent().get(0).getFullProductName());

    verify(productRepository).getCriteria(any(ProductCriteriaDTO.class));
    verify(productRepository).findByFilter(any(Criteria.class), eq(pageable));
    verify(productRepository).getCount(any(Criteria.class));
  }

  @Test
  void fetchProductsByFilters_empty() {
    Pageable pageable = PageRequest.of(0, 5);

    when(productRepository.getCriteria(any())).thenReturn(new Criteria());
    when(productRepository.findByFilter(any(), any())).thenReturn(List.of());
    when(productRepository.getCount(any())).thenReturn(0L);

    ProductListDTO dto = productService.fetchProductsByFilters(
      ORG_ID, null, null, null, null,null, null, null, null, null, null, null, pageable, null
    );

    assertEquals(0, dto.getContent().size());
    assertEquals(0, dto.getTotalElements());
    assertEquals(0, dto.getTotalPages());
  }

  @Test
  void fetchProductsByFilters_repositoryThrows_propagates() {
    Pageable pageable = PageRequest.of(0, 2);

    when(productRepository.getCriteria(any())).thenReturn(new Criteria());
    when(productRepository.findByFilter(any(), any())).thenThrow(new RuntimeException("DB error"));

    RuntimeException ex = assertThrows(RuntimeException.class, () ->
      productService.fetchProductsByFilters(ORG_ID, null, null, null,null, null, null, null, null, null, null, null, pageable, null)
    );
    assertEquals("DB error", ex.getMessage());
  }

  // ---------------- update KO ----------------

  @Test
  void updateStatuses_someProductsMissing_returnsKO_PRODUCT_NOT_FOUND() {
    ProductUpdateStatusRequestDTO req = req(List.of("g1","g2"),
      ProductStatus.SUPERVISED);

    when(initiativeConfigMap.get("initiId")).thenReturn(initiativeConfig());
    when(productRepository.findByGtinCodeInAndInitiativeId(req.getGtinCodes(),"initiId"))
      .thenReturn(List.of());

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(
      "initiId",req,UserRole.INVITALIA.getRole(),USERNAME);

    assertEquals("KO",res.getStatus());
    assertEquals(PRODUCT_NOT_FOUND_ERROR_KEY,res.getErrorKey());

    verifyNoInteractions(notificationService);
  }

  @Test
  void updateStatuses_mixedStatuses_returnsKO() {
    ProductUpdateStatusRequestDTO req = req(List.of("g1","g2"),
      ProductStatus.SUPERVISED);

    Product a = Product.builder().gtinCode("g1").status(ProductStatus.UPLOADED.name()).build();
    Product b = Product.builder().gtinCode("g2").status(ProductStatus.SUPERVISED.name()).build();

    when(initiativeConfigMap.get("initiId")).thenReturn(initiativeConfig());
    when(productRepository.findByGtinCodeInAndInitiativeId(any(),any()))
      .thenReturn(List.of(a,b));

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(
      "initiId",req,UserRole.INVITALIA.getRole(),USERNAME);

    assertEquals("KO",res.getStatus());
    assertEquals(MIXED_STATUS_ERROR_KEY,res.getErrorKey());

    verifyNoInteractions(notificationService);
  }

  @Test
  void updateStatuses_invalidCurrentStatus_returnsKO() {
    ProductUpdateStatusRequestDTO req = req(List.of("g1","g2"),
      ProductStatus.SUPERVISED);

    Product a = Product.builder().gtinCode("g1").status(ProductStatus.SUPERVISED.name()).build();
    Product b = Product.builder().gtinCode("g2").status(ProductStatus.SUPERVISED.name()).build();

    when(initiativeConfigMap.get("initiId")).thenReturn(initiativeConfig());
    when(productRepository.findByGtinCodeInAndInitiativeId(any(),any()))
      .thenReturn(List.of(a,b));

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(
      "initiId",req,UserRole.INVITALIA.getRole(),USERNAME);

    assertEquals("KO",res.getStatus());
    assertEquals(INVALID_CURRENT_STATUS_ERROR_KEY,res.getErrorKey());

    verifyNoInteractions(notificationService);
  }

  @Test
  void updateStatuses_initiativeConfigNotFound_returnsKO() {
    ProductUpdateStatusRequestDTO req = req(List.of("g1","g2"),
      ProductStatus.SUPERVISED);


    when(initiativeConfigMap.get("initiId")).thenReturn(null);

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(
      "initiId",req,UserRole.INVITALIA.getRole(),USERNAME);

    assertEquals("KO",res.getStatus());
    assertEquals(INITIATIVE_NOT_FOUND_ERROR_KEY,res.getErrorKey());

    verifyNoInteractions(notificationService);
  }


  @Test
  void updateStatuses_transactionNotAllowed_returnsKO() {
    ProductUpdateStatusRequestDTO req = req(List.of("g1","g2"),
      ProductStatus.APPROVED);

    when(initiativeConfigMap.get("initiId")).thenReturn(initiativeConfig());

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(
      "initiId",req,UserRole.INVITALIA.getRole(),USERNAME);

    assertEquals("KO",res.getStatus());
    assertEquals(TRANSITION_NOT_ALLOWED_ERROR_KEY,res.getErrorKey());

    verifyNoInteractions(notificationService);
  }

  // ---------------- update OK no email ----------------

  @Test
  void updateStatuses_OK_noEmail() {
    ProductUpdateStatusRequestDTO req = req(List.of("p1","p2"),
      ProductStatus.WAIT_APPROVED);

    Product p1 = Product.builder().gtinCode("p1").status(ProductStatus.UPLOADED.name())
      .statusChangeChronology(new ArrayList<>()).build();

    Product p2 = Product.builder().gtinCode("p2").status(ProductStatus.UPLOADED.name())
      .statusChangeChronology(new ArrayList<>()).build();

    when(initiativeConfigMap.get("initiId")).thenReturn(initiativeConfig());
    when(productRepository.findByGtinCodeInAndInitiativeId(any(),any()))
      .thenReturn(List.of(p1,p2));

    when(productRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(
      "initiId",req,UserRole.INVITALIA.getRole(),USERNAME);

    assertEquals("OK",res.getStatus());

    verifyNoInteractions(notificationService);
  }

  // ---------------- update OK rejected email success ----------------

  @Test
  void updateStatuses_rejected_allEmailsOK() {
    ProductUpdateStatusRequestDTO req = req(List.of("g1","g2"),
      ProductStatus.REJECTED);

    Product a = Product.builder().gtinCode("g1").status(ProductStatus.UPLOADED.name()).build();
    Product b = Product.builder().gtinCode("g2").status(ProductStatus.UPLOADED.name()).build();

    when(initiativeConfigMap.get("initiId")).thenReturn(initiativeConfig());
    when(productRepository.findByGtinCodeInAndInitiativeId(any(),any()))
      .thenReturn(List.of(a,b));

    when(productRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
    when(productRepository.getProductNamesGroupedByEmail(any(),any()))
      .thenReturn(List.of(
        EmailProductDTO.builder().email("a@mail.it").productNames(List.of("n1")).build()
      ));

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(
      "initiId",req,UserRole.INVITALIA.getRole(),USERNAME);

    assertEquals("OK",res.getStatus());

    verify(notificationService).sendEmailUpdateStatus(
      List.of("n1"),"FORMAL",ProductStatus.REJECTED.name(),"a@mail.it");
  }

  // ---------------- update OK even if email fails ----------------

  @Test
  void updateStatuses_rejected_oneEmailFails_returnsOK() {
    ProductUpdateStatusRequestDTO req = req(List.of("g1"),
      ProductStatus.REJECTED);

    Product a = Product.builder().gtinCode("g1").status(ProductStatus.UPLOADED.name()).build();

    when(initiativeConfigMap.get("initiId")).thenReturn(initiativeConfig());
    when(productRepository.findByGtinCodeInAndInitiativeId(any(),any()))
      .thenReturn(List.of(a));

    when(productRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
    when(productRepository.getProductNamesGroupedByEmail(any(),any()))
      .thenReturn(List.of(
        EmailProductDTO.builder().email("ko@mail.it").productNames(List.of("n")).build()
      ));

    doThrow(new RuntimeException("fail"))
      .when(notificationService)
      .sendEmailUpdateStatus(anyList(),anyString(),anyString(),anyString());

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(
      "initiId",req,UserRole.INVITALIA.getRole(),USERNAME);

    assertEquals("OK",res.getStatus());
    assertNull(res.getErrorKey());

    verify(notificationService).sendEmailUpdateStatus(any(),any(),any(),any());
  }

  // ---------------- helper ----------------

  private ProductUpdateStatusRequestDTO req(List<String> gtins,
                                            ProductStatus target) {
    ProductUpdateStatusRequestDTO dto = new ProductUpdateStatusRequestDTO();
    dto.setGtinCodes(gtins);
    dto.setCurrentStatus(ProductStatus.UPLOADED);
    dto.setTargetStatus(target);
    dto.setMotivation("why");
    dto.setFormalMotivation("FORMAL");
    return dto;
  }

  private InitiativeConfig initiativeConfig() {
    InitiativeConfig config = new InitiativeConfig();
    config.setInitiativeName("Initiative");

    config.setStateTransitions(Map.of(
      UserRole.INVITALIA.getRole(),
      transitions(
        ProductStatus.WAIT_APPROVED.name(), List.of(ProductStatus.UPLOADED),
        ProductStatus.REJECTED.name(), List.of(ProductStatus.UPLOADED),
        ProductStatus.SUPERVISED.name(), List.of(ProductStatus.UPLOADED)
      )
    ));
    return config;
  }

  @SuppressWarnings({"rawtypes","unchecked"})
  private Map<String, List<ProductStatus>> transitions(Object... entries) {
    Map map = new HashMap();
    for (int i = 0; i < entries.length; i += 2) {
      map.put(entries[i], entries[i+1]);
    }
    return map;
  }
}
