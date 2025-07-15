package it.gov.pagopa.register.repository;


import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductSpecificRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductFileSpecificRepositoryTest {


  @Mock
  private MongoTemplate mongoTemplate;

  private ProductSpecificRepositoryImpl productSpecificRepository;

  @BeforeEach
  void setUp() {
    productSpecificRepository = new ProductSpecificRepositoryImpl(mongoTemplate);
  }

  @Test
  void testFindByFilter_withPageable() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    Pageable pageable = PageRequest.of(0, 10);
    Product product = new Product();
    when(mongoTemplate.find(any(Query.class), eq(Product.class)))
      .thenReturn(List.of(product));

    List<Product> result = productSpecificRepository.findByFilter(criteria, pageable);

    assertEquals(1, result.size());
    verify(mongoTemplate).find(any(Query.class), eq(Product.class));
  }



  @Test
  void testGetCriteria_AllFieldsPresent() {
    Criteria criteria = productSpecificRepository.getCriteria(
      "org1", "cat", "prodCode", "fileId", "eprel", "gtin");

    assertNotNull(criteria);
    assertTrue(criteria.getCriteriaObject().containsKey("organizationId"));
    assertTrue(criteria.getCriteriaObject().containsKey("category"));
    assertTrue(criteria.getCriteriaObject().containsKey("productCode"));
    assertTrue(criteria.getCriteriaObject().containsKey("productFileId"));
    assertTrue(criteria.getCriteriaObject().containsKey("eprelCode"));
    assertTrue(criteria.getCriteriaObject().containsKey("gtinCode"));
  }

  @Test
  void testGetCriteria_OnlyOrgIdPresent() {
    Criteria criteria = productSpecificRepository.getCriteria(
      "org1", null, null, null, null, null);

    assertEquals("org1", criteria.getCriteriaObject().get("organizationId"));
    assertEquals(1, criteria.getCriteriaObject().size());
  }

  @Test
  void testGetCount_ReturnsCorrectValue() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(5L);

    Long count = productSpecificRepository.getCount(criteria);

    assertEquals(5L, count);
    ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).count(captor.capture(), eq(Product.class));

    Query capturedQuery = captor.getValue();
    assertNotNull(capturedQuery.getQueryObject().get("organizationId"));
  }




}
