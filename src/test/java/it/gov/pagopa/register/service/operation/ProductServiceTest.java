package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.notification.NotificationServiceImpl;
import it.gov.pagopa.register.dto.operation.EmailProductDTO;
import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.dto.operation.ProductUpdateStatusRequestDTO;
import it.gov.pagopa.register.dto.operation.UpdateResultDTO;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.USERNAME;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.WASHINGMACHINES;
import static it.gov.pagopa.register.utils.ObjectMaker.buildStatusChangeEventsList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private NotificationServiceImpl notificationService;

  @InjectMocks
  private ProductService productService;


  @Test
  void testGetProductsByPage_Success() {
    String organizationId = "org123";
    Pageable pageable = PageRequest.of(0, 2);

    Product product1 = Product.builder()
      .organizationId("organizationId")
      .status(ProductStatus.WAIT_APPROVED.name())
      .productFileId("product1")
      .category(WASHINGMACHINES)
      .capacity("N\\A")
      .build();
    Product product2 = Product.builder()
      .organizationId("organizationId")
      .status(ProductStatus.SUPERVISED.name())
      .productFileId("product2")
      .category(WASHINGMACHINES)
      .capacity("N\\A")
      .build();

    List<Product> productList = Arrays.asList(product1, product2);


    Criteria criteria = Criteria.where(Product.Fields.organizationId).is(organizationId);

    when(productRepository.getCriteria(
      organizationId,
      null,
      null,
      null,
      null,
      null,
      null))
      .thenReturn(criteria);

    when(productRepository.findByFilter(criteria, pageable))
      .thenReturn(productList);


    ProductListDTO response = productService.fetchProductsByFilters(
      organizationId,
      null,
      null,
      null,
      null,
      null,
      null,
      pageable,
      UserRole.OPERATORE.getRole());

    assertEquals(2, response.getContent().size());
    assertEquals(0, response.getPageNo());
    assertEquals(2, response.getPageSize());
    assertEquals(2, response.getTotalElements());
    assertEquals(1, response.getTotalPages());

    verify(productRepository, times(1))
      .findByFilter(any(Criteria.class), eq(pageable));

  }

  @Test
  void testGetProductsByPage_EmptyPage() {
    String organizationId = "org123";
    Pageable pageable = PageRequest.of(0, 2);



    List<Product> productList = Collections.emptyList();


    when(productRepository.findByFilter(any(), any()))
      .thenReturn(productList);


    ProductListDTO response = productService.fetchProductsByFilters(
      organizationId,
      null,
      null,
      null,
      null,
      null,
      null,
      pageable,
      null);

    assertEquals(0, response.getContent().size());
    assertEquals(0, response.getPageNo());
    assertEquals(2, response.getPageSize());
    assertEquals(0, response.getTotalElements());
    assertEquals(0, response.getTotalPages());

    verify(productRepository, times(1))
      .findByFilter(any(), any());

  }

  @Test
  void testGetProductsByPage_RepositoryThrowsException() {
    String organizationId = "org123";
    Pageable pageable = PageRequest.of(0, 2);


    when(productRepository.findByFilter(any(), any()))
      .thenThrow(new RuntimeException("Database error"));

    RuntimeException exception = assertThrows(RuntimeException.class, () -> productService.fetchProductsByFilters(organizationId, null,  null, null, null, null,null,pageable, null));

    assertEquals("Database error", exception.getMessage());

    verify(productRepository, times(1))
      .findByFilter(any(), any());

  }

  @Test
  void testUpdateProductStatuses_Success() {
    String organizationId = "org123";
    List<String> productIds = List.of("prod1", "prod2");

    Product product1 = Product.builder()
      .gtinCode("prod1")
      .organizationId(organizationId)
      .status("WAIT_APPROVED")
      .productName("name1")
      .productFileId("file1")
      .statusChangeChronology(buildStatusChangeEventsList())
      .build();

    Product product2 = Product.builder()
      .gtinCode("prod2")
      .organizationId(organizationId)
      .status("WAIT_APPROVED")
      .productName("name2")
      .productFileId("file1")
      .statusChangeChronology(buildStatusChangeEventsList())
      .build();

    ProductUpdateStatusRequestDTO requestDTO = new ProductUpdateStatusRequestDTO();
    requestDTO.setGtinCodes(productIds);
    requestDTO.setCurrentStatus(ProductStatus.WAIT_APPROVED);
    requestDTO.setTargetStatus(ProductStatus.APPROVED);
    requestDTO.setMotivation("Valid reason");
    requestDTO.setFormalMotivation("Valid formal reason");

    List<Product> productList = List.of(product1, product2);
    when(productRepository.findUpdatableProducts(productIds, ProductStatus.WAIT_APPROVED,ProductStatus.APPROVED, UserRole.INVITALIA_ADMIN.getRole()))
      .thenReturn(productList);

    when(productRepository.saveAll(productList))
      .thenReturn(productList);
    UpdateResultDTO result = productService.updateProductStatusesWithNotification(
      requestDTO,
      UserRole.INVITALIA_ADMIN.getRole(),
      USERNAME
    );


    assertEquals("OK",result.getStatus());

    verify(productRepository).findUpdatableProducts(productIds, ProductStatus.WAIT_APPROVED, ProductStatus.APPROVED, UserRole.INVITALIA_ADMIN.getRole());
    verify(productRepository).saveAll(productList);

  }
  @Test
  void testUpdateProductStatuses_EmailFailure() {
    String organizationId = "org123";
    String motivation = "motivation";
    List<String> productIds = List.of("prod1", "prod2");

    Product product1 = Product.builder()
      .gtinCode("prod1")
      .organizationId(organizationId)
      .status(ProductStatus.UPLOADED.name())
      .productName("name1")
      .productFileId("file1")
      .statusChangeChronology(buildStatusChangeEventsList())
      .build();

    Product product2 = Product.builder()
      .gtinCode("prod2")
      .organizationId(organizationId)
      .status(ProductStatus.UPLOADED.name())
      .productName("name2")
      .productFileId("file1")
      .statusChangeChronology(buildStatusChangeEventsList())
      .build();

    EmailProductDTO emailProductDTO = EmailProductDTO.builder()
      .id("test@gmail.com")
      .productNames(List.of("name1", "name2"))
      .build();

    List<Product> productList = List.of(product1, product2);
    List<EmailProductDTO> emailProductDTOs = List.of(emailProductDTO);

    when(productRepository.findUpdatableProducts(productIds, ProductStatus.UPLOADED, ProductStatus.REJECTED, UserRole.INVITALIA_ADMIN.getRole()))
      .thenReturn(productList);

    when(productRepository.saveAll(productList))
      .thenReturn(productList);

    when(productRepository.getProductNamesGroupedByEmail(productList.stream().map(Product::getGtinCode).toList()))
      .thenReturn(emailProductDTOs);

    doThrow(new RuntimeException("Email service error")).when(notificationService)
      .sendEmailUpdateStatus(List.of("name1", "name2"), motivation, ProductStatus.REJECTED.name(), "test@gmail.com");

    ProductUpdateStatusRequestDTO requestDTO = new ProductUpdateStatusRequestDTO();
    requestDTO.setGtinCodes(productIds);
    requestDTO.setCurrentStatus(ProductStatus.UPLOADED);
    requestDTO.setTargetStatus(ProductStatus.REJECTED);
    requestDTO.setMotivation("Valid reason");
    requestDTO.setFormalMotivation("Valid formal reason");

    UpdateResultDTO result = productService.updateProductStatusesWithNotification(
      requestDTO,
      UserRole.INVITALIA_ADMIN.getRole(),
      USERNAME
    );

    assertEquals("KO",result.getStatus());
  }

}
