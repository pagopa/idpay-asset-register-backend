package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.notification.NotificationService;
import it.gov.pagopa.register.dto.operation.*;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.StatusChangeEvent;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.UpdateKeyConstant.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private ProductService productService;

  private static final String ORG_ID = "org123";

  // ---------------- fetchProductsByFilters ----------------

  @Test
  void fetchProductsByFilters_happyPath() {
    Pageable pageable = PageRequest.of(0, 2);

    Product p1 = Product.builder()
      .organizationId(ORG_ID).status(ProductStatus.UPLOADED.name())
      .brand("B1").model("M1").category("C1")
      .productName("C1 B1 M1")
      .registrationDate(LocalDateTime.now())
      .build();

    Product p2 = Product.builder()
      .organizationId(ORG_ID).status(ProductStatus.UPLOADED.name())
      .brand("B2").model("M2").category("C2")
      .productName("C2 B2 M2")
      .registrationDate(LocalDateTime.now())
      .build();

    when(productRepository.getCriteria(any(ProductCriteriaDTO.class))).thenReturn(new Criteria());
    when(productRepository.findByFilter(any(Criteria.class), eq(pageable))).thenReturn(List.of(p1, p2));
    when(productRepository.getCount(any(Criteria.class))).thenReturn(2L);

    ProductListDTO dto = productService.fetchProductsByFilters(
      ORG_ID, null, null, null, null, null, null, null, ProductStatus.UPLOADED.name(),
      pageable, UserRole.INVITALIA.getRole()
    );

    assertNotNull(dto);
    assertEquals(2, dto.getContent().size());
    assertEquals(0, dto.getPageNo());
    assertEquals(2, dto.getPageSize());
    assertEquals(2, dto.getTotalElements());
    assertEquals(1, dto.getTotalPages());
    assertEquals("C1 B1 M1", dto.getContent().get(0).getProductName());

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
      ORG_ID, null, null, null, null, null, null, null, null, pageable, null
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
      productService.fetchProductsByFilters(ORG_ID, null, null, null, null, null, null, null, null, pageable, null)
    );
    assertEquals("DB error", ex.getMessage());
  }

  // ---------------- updateProductStatusesWithNotification: KO paths ----------------

  @Test
  void updateStatuses_someProductsMissing_returnsKO_PRODUCT_NOT_FOUND() {
    ProductUpdateStatusRequestDTO req = req(List.of("g1", "g2"),
      ProductStatus.UPLOADED, ProductStatus.APPROVED, "why", "FORMAL");

    when(productRepository.findByIds(req.getGtinCodes()))
      .thenReturn(List.of(Product.builder().gtinCode("g1").status(ProductStatus.UPLOADED.name()).build())); // manca g2

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(req, UserRole.INVITALIA.getRole(), USERNAME);
    assertEquals("KO", res.getStatus());
    assertEquals(PRODUCT_NOT_FOUND_ERROR_KEY, res.getErrorKey());
    verify(productRepository).findByIds(req.getGtinCodes());
    verifyNoMoreInteractions(productRepository, notificationService);
  }

  @Test
  void updateStatuses_mixedCurrentStatuses_returnsKO_MIXED_STATUS() {
    ProductUpdateStatusRequestDTO req = req(List.of("g1", "g2"),
      ProductStatus.UPLOADED, ProductStatus.APPROVED, "why", "FORMAL");

    Product a = Product.builder().gtinCode("g1").status(ProductStatus.UPLOADED.name()).build();
    Product b = Product.builder().gtinCode("g2").status(ProductStatus.SUPERVISED.name()).build();

    when(productRepository.findByIds(req.getGtinCodes())).thenReturn(List.of(a, b));

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(req, UserRole.INVITALIA.getRole(), USERNAME);
    assertEquals("KO", res.getStatus());
    assertEquals(MIXED_STATUS_ERROR_KEY, res.getErrorKey());
    verify(productRepository).findByIds(req.getGtinCodes());
    verifyNoMoreInteractions(productRepository, notificationService);
  }

  @Test
  void updateStatuses_currentStatusMismatch_returnsKO_INVALID_CURRENT_STATUS() {
    ProductUpdateStatusRequestDTO req = req(List.of("g1", "g2"),
      ProductStatus.UPLOADED, ProductStatus.APPROVED, "why", "FORMAL");

    Product a = Product.builder().gtinCode("g1").status(ProductStatus.SUPERVISED.name()).build();
    Product b = Product.builder().gtinCode("g2").status(ProductStatus.SUPERVISED.name()).build();

    when(productRepository.findByIds(req.getGtinCodes())).thenReturn(List.of(a, b));

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(req, UserRole.INVITALIA.getRole(), USERNAME);
    assertEquals("KO", res.getStatus());
    assertEquals(INVALID_CURRENT_STATUS_ERROR_KEY, res.getErrorKey());
    verify(productRepository).findByIds(req.getGtinCodes());
    verifyNoMoreInteractions(productRepository, notificationService);
  }

  @Test
  void updateStatuses_transitionNotAllowed_returnsKO_TRANSITION_NOT_ALLOWED() {
    ProductUpdateStatusRequestDTO req = req(List.of("g1", "g2"),
      ProductStatus.UPLOADED, ProductStatus.APPROVED, "why", "FORMAL");

    Product a = Product.builder().gtinCode("g1").status(ProductStatus.UPLOADED.name()).build();
    Product b = Product.builder().gtinCode("g2").status(ProductStatus.UPLOADED.name()).build();

    when(productRepository.findByIds(req.getGtinCodes())).thenReturn(List.of(a, b));
    when(productRepository.getAllowedInitialStates(ProductStatus.APPROVED, UserRole.INVITALIA.getRole()))
      .thenReturn(List.of(ProductStatus.SUPERVISED.name())); // non contiene UPLOADED

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(req, UserRole.INVITALIA.getRole(), USERNAME);
    assertEquals("KO", res.getStatus());
    assertEquals(TRANSITION_NOT_ALLOWED_ERROR_KEY, res.getErrorKey());
    verify(productRepository).getAllowedInitialStates(ProductStatus.APPROVED, UserRole.INVITALIA.getRole());
  }

  // ---------------- updateProductStatusesWithNotification: OK (no email) ----------------

  @Test
  void updateStatuses_targetNotRejected_noEmail_OK_andChronology_L1() {
    ProductUpdateStatusRequestDTO req = req(List.of("p1", "p2"),
      ProductStatus.UPLOADED, ProductStatus.APPROVED, "why", "FORMAL_OK");

    Product p1 = Product.builder().gtinCode("p1").status(ProductStatus.UPLOADED.name())
      .statusChangeChronology(new ArrayList<>()).build();
    Product p2 = Product.builder().gtinCode("p2").status(ProductStatus.UPLOADED.name())
      .statusChangeChronology(new ArrayList<>()).build();

    when(productRepository.findByIds(req.getGtinCodes())).thenReturn(List.of(p1, p2));
    when(productRepository.getAllowedInitialStates(ProductStatus.APPROVED, UserRole.INVITALIA.getRole()))
      .thenReturn(List.of(ProductStatus.UPLOADED.name()));
    when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(req, UserRole.INVITALIA.getRole(), USERNAME);
    assertEquals("OK", res.getStatus());

    // <-- usando CAPTOR, niente Iterable.get()
    ArgumentCaptor<List<Product>> captor = ArgumentCaptor.forClass(List.class);
    verify(productRepository).saveAll(captor.capture());
    List<Product> saved = captor.getValue();
    assertEquals(2, saved.size());
    saved.forEach(p -> {
      assertEquals(ProductStatus.APPROVED.name(), p.getStatus());
      assertEquals("FORMAL_OK", p.getFormalMotivation());
      assertNotNull(p.getStatusChangeChronology());
      assertFalse(p.getStatusChangeChronology().isEmpty());
      StatusChangeEvent last = p.getStatusChangeChronology().get(p.getStatusChangeChronology().size() - 1);
      assertEquals("L1", last.getRole());
      assertEquals(ProductStatus.UPLOADED, last.getCurrentStatus());
      assertEquals(ProductStatus.APPROVED, last.getTargetStatus());
      assertEquals("why", last.getMotivation());
      assertEquals(USERNAME, last.getUsername());
    });

    verifyNoInteractions(notificationService);
  }

  // ---------------- updateProductStatusesWithNotification: OK (rejected + email OK) ----------------

  @Test
  void updateStatuses_targetRejected_allEmailsOK_returnsOK_andUsesFormalMotivation() {
    ProductUpdateStatusRequestDTO req = req(List.of("g1", "g2"),
      ProductStatus.UPLOADED, ProductStatus.REJECTED, "why", "FORMAL_MAIL");

    Product a = Product.builder().gtinCode("g1").status(ProductStatus.UPLOADED.name()).build();
    Product b = Product.builder().gtinCode("g2").status(ProductStatus.UPLOADED.name()).build();

    when(productRepository.findByIds(req.getGtinCodes())).thenReturn(List.of(a, b));
    when(productRepository.getAllowedInitialStates(ProductStatus.REJECTED, UserRole.INVITALIA.getRole()))
      .thenReturn(List.of(ProductStatus.UPLOADED.name()));
    when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    when(productRepository.getProductNamesGroupedByEmail(List.of("g1", "g2"))).thenReturn(List.of(
      EmailProductDTO.builder().id("a@mail.it").productNames(List.of("n1", "n2")).build(),
      EmailProductDTO.builder().id("b@mail.it").productNames(List.of("n3")).build()
    ));

    doNothing().when(notificationService).sendEmailUpdateStatus(anyList(), anyString(), anyString(), anyString());

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(req, UserRole.INVITALIA.getRole(), USERNAME);
    assertEquals("OK", res.getStatus());

    verify(productRepository).getProductNamesGroupedByEmail(List.of("g1", "g2"));
    // niente eq(...) superflui: tutti letterali specifici
    verify(notificationService).sendEmailUpdateStatus(List.of("n1", "n2"), "FORMAL_MAIL", ProductStatus.REJECTED.name(), "a@mail.it");
    verify(notificationService).sendEmailUpdateStatus(List.of("n3"), "FORMAL_MAIL", ProductStatus.REJECTED.name(), "b@mail.it");
  }

  // ---------------- updateProductStatusesWithNotification: KO su email ----------------

  @Test
  void updateStatuses_targetRejected_oneEmailFails_returnsKO_EMAIL_ERROR() {
    ProductUpdateStatusRequestDTO req = req(List.of("g1", "g2"),
      ProductStatus.UPLOADED, ProductStatus.REJECTED, "why", "FORMAL_MAIL");

    Product a = Product.builder().gtinCode("g1").status(ProductStatus.UPLOADED.name()).build();
    Product b = Product.builder().gtinCode("g2").status(ProductStatus.UPLOADED.name()).build();

    when(productRepository.findByIds(req.getGtinCodes())).thenReturn(List.of(a, b));
    when(productRepository.getAllowedInitialStates(ProductStatus.REJECTED, UserRole.INVITALIA.getRole()))
      .thenReturn(List.of(ProductStatus.UPLOADED.name()));
    when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    when(productRepository.getProductNamesGroupedByEmail(List.of("g1", "g2"))).thenReturn(List.of(
      EmailProductDTO.builder().id("ok@mail.it").productNames(List.of("nOK")).build(),
      EmailProductDTO.builder().id("ko@mail.it").productNames(List.of("nKO")).build()
    ));

    doNothing().when(notificationService)
      .sendEmailUpdateStatus(List.of("nOK"), "FORMAL_MAIL", ProductStatus.REJECTED.name(), "ok@mail.it");
    doThrow(new RuntimeException("Email service error"))
      .when(notificationService)
      .sendEmailUpdateStatus(List.of("nKO"), "FORMAL_MAIL", ProductStatus.REJECTED.name(), "ko@mail.it");

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(req, UserRole.INVITALIA.getRole(), USERNAME);
    assertEquals("KO", res.getStatus());
    assertEquals(EMAIL_ERROR_KEY, res.getErrorKey());
    verify(notificationService, times(2)).sendEmailUpdateStatus(anyList(), anyString(), anyString(), anyString());
  }

  // ---------------- ruolo L2 ----------------

  @Test
  void updateStatuses_roleNotInvitalia_chronologyRoleIsL2() {
    ProductUpdateStatusRequestDTO req = req(List.of("x1"),
      ProductStatus.UPLOADED, ProductStatus.SUPERVISED, "mot", "FORMAL");

    Product p = Product.builder().gtinCode("x1").status(ProductStatus.UPLOADED.name())
      .statusChangeChronology(new ArrayList<>()).build();

    when(productRepository.findByIds(req.getGtinCodes())).thenReturn(List.of(p));
    when(productRepository.getAllowedInitialStates(ProductStatus.SUPERVISED, UserRole.OPERATORE.getRole()))
      .thenReturn(List.of(ProductStatus.UPLOADED.name()));
    when(productRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

    UpdateResultDTO res = productService.updateProductStatusesWithNotification(req, UserRole.OPERATORE.getRole(), USERNAME);
    assertEquals("OK", res.getStatus());

    // uso CAPTOR per evitare Iterable
    ArgumentCaptor<List<Product>> captor = ArgumentCaptor.forClass(List.class);
    verify(productRepository).saveAll(captor.capture());
    List<Product> saved = captor.getValue();
    Product savedP = saved.get(0);
    StatusChangeEvent last = savedP.getStatusChangeChronology().get(savedP.getStatusChangeChronology().size() - 1);
    assertEquals("L2", last.getRole());
    assertEquals(ProductStatus.UPLOADED, last.getCurrentStatus());
    assertEquals(ProductStatus.SUPERVISED, last.getTargetStatus());
    assertEquals("mot", last.getMotivation());

    verifyNoInteractions(notificationService);
  }

  // ---------------- helper ----------------

  private ProductUpdateStatusRequestDTO req(List<String> gtins,
                                            ProductStatus current,
                                            ProductStatus target,
                                            String motivation,
                                            String formalMotivation) {
    ProductUpdateStatusRequestDTO dto = new ProductUpdateStatusRequestDTO();
    dto.setGtinCodes(gtins);
    dto.setCurrentStatus(current);
    dto.setTargetStatus(target);
    dto.setMotivation(motivation);
    dto.setFormalMotivation(formalMotivation);
    return dto;
  }
}
