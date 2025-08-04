package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.notification.NotificationServiceImpl;
import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.dto.operation.UpdateResultDTO;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
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
import java.util.Optional;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.WASHINGMACHINES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private ProductFileRepository productFileRepository;

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
      .productFileId("product1")
      .category(WASHINGMACHINES)
      .capacity("N\\A")
      .build();
    Product product2 = Product.builder()
      .organizationId("organizationId")
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


    ProductListDTO response = productService.getProducts(
      organizationId,
      null,
      null,
      null,
      null,
      null,
      null,
      pageable);

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


    ProductListDTO response = productService.getProducts(
      organizationId,
      null,
      null,
      null,
      null,
      null,
      null,
      pageable);

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

    RuntimeException exception = assertThrows(RuntimeException.class, () -> productService.getProducts(organizationId, null,  null, null, null, null,null,pageable));

    assertEquals("Database error", exception.getMessage());

    verify(productRepository, times(1))
      .findByFilter(any(), any());

  }

  @Test
  void testUpdateProductStatuses_Success() {
    String organizationId = "org123";
    String motivation = "motivation";
    List<String> productIds = List.of("prod1", "prod2");

    Product product1 = Product.builder()
      .gtinCode("prod1")
      .organizationId(organizationId)
      .status("REJECTED")
      .productFileId("file1")
      .build();

    Product product2 = Product.builder()
      .gtinCode("prod2")
      .organizationId(organizationId)
      .status("REJECTED")
      .productFileId("file1")
      .build();

    List<Product> productList = List.of(product1, product2);

    ProductFile productFile = ProductFile.builder()
      .id("file1")
      .userEmail("user@example.com")
      .build();

    when(productRepository.findByIdsAndOrganizationIdAndNeStatus(productIds, organizationId,"APPROVED"))
      .thenReturn(productList);

    when(productRepository.saveAll(productList))
      .thenReturn(productList);

    when(productFileRepository.findById("file1"))
      .thenReturn(Optional.of(productFile));

    doNothing().when(notificationService)
      .sendEmailUpdateStatus(anyList(), eq(motivation), eq("APPROVED"), eq("user@example.com"));

    UpdateResultDTO result = productService.updateProductState(
      organizationId,
      productIds,
      ProductStatus.APPROVED.name(),
      motivation
    );


    assertEquals("OK",result.getStatus());

    verify(productRepository).findByIdsAndOrganizationIdAndNeStatus(productIds, organizationId,"APPROVED");
    verify(productRepository).saveAll(productList);
    verify(productFileRepository).findById("file1");
    verify(notificationService).sendEmailUpdateStatus(
      List.of("prod1", "prod2"),
      motivation,
      "APPROVED",
      "user@example.com"
    );
  }


}
