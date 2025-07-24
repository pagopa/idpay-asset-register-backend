package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.enums.UploadCsvStatus;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ProductServiceTest {
  @Mock
  private ProductRepository productRepository;

  @InjectMocks
  private ProductService productService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testGetProductsByPage_Success() {
    String organizationId = "org123";
    Pageable pageable = PageRequest.of(0, 2);

    Product product1 = Product.builder()
      .organizationId("organizationId")
      .productFileId("product1")
      .capacity("N\\A")
      .build();
    Product product2 = Product.builder()
      .organizationId("organizationId")
      .productFileId("product2")
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

    RuntimeException exception = assertThrows(RuntimeException.class, () -> productService.getProducts(organizationId, null, null, null, null, null, pageable));

    assertEquals("Database error", exception.getMessage());

    verify(productRepository, times(1))
      .findByFilter(any(), any());

  }

}
