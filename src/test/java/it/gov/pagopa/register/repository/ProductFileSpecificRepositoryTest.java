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
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

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
      "org1", "cat",  "fileId", "eprel", "gtin","productName","status");

    assertNotNull(criteria);
    assertTrue(criteria.getCriteriaObject().containsKey("organizationId"));
    assertTrue(criteria.getCriteriaObject().containsKey("category"));
    assertTrue(criteria.getCriteriaObject().containsKey("productFileId"));
    assertTrue(criteria.getCriteriaObject().containsKey("eprelCode"));
    assertTrue(criteria.getCriteriaObject().containsKey("_id"));
    assertTrue(criteria.getCriteriaObject().containsKey("productName"));
    assertTrue(criteria.getCriteriaObject().containsKey("status"));
  }

  @Test
  void testGetCriteria_OnlyOrgIdPresent() {
    Criteria criteria = productSpecificRepository.getCriteria(
      "org1", null, null, null, null, null,null);

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

  @Test
  void testFindDistinctProductFileIdAndCategoryByOrganizationId() {
    String orgId = "org123";
    Product product = Product.builder()
      .productFileId("file123")
      .category("catA")
      .build();

    List<Product> products = List.of(product);
    AggregationResults<Product> aggregationResults = new AggregationResults<>(products, new org.bson.Document());

    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(aggregationResults);

    List<Product> result = productSpecificRepository.retrieveDistinctProductFileIdsBasedOnRole(orgId,"operatore");

    assertEquals(1, result.size());
    assertEquals("file123", result.get(0).getProductFileId());
    assertEquals("catA", result.get(0).getCategory());

    verify(mongoTemplate).aggregate(any(Aggregation.class), eq("product"), eq(Product.class));
  }

  @Test
  void testFindByFilter_SortByBatchNameAsc() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    Pageable pageable = PageRequest.of(0, 10, Sort.by("batchName").ascending());

    AggregationResults<Product> aggregationResults = mock(AggregationResults.class);
    when(aggregationResults.getMappedResults()).thenReturn(List.of(Product.builder().build()));

    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(aggregationResults);

    List<Product> result = productSpecificRepository.findByFilter(criteria, pageable);

    assertNotNull(result);
    assertEquals(1, result.size());

    ArgumentCaptor<Aggregation> aggregationCaptor = ArgumentCaptor.forClass(Aggregation.class);
    verify(mongoTemplate).aggregate(aggregationCaptor.capture(), eq("product"), eq(Product.class));

    Aggregation usedAggregation = aggregationCaptor.getValue();
    assertNotNull(usedAggregation);
  }



  @Test
  void testFindByFilter_SortByBatchNameDesc() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    Pageable pageable = PageRequest.of(0, 10, Sort.by("batchName").descending());

    AggregationResults<Product> aggregationResults = mock(AggregationResults.class);
    when(aggregationResults.getMappedResults()).thenReturn(List.of(Product.builder().build()));

    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(aggregationResults);

    List<Product> result = productSpecificRepository.findByFilter(criteria, pageable);

    assertNotNull(result);
    assertEquals(1, result.size());

    ArgumentCaptor<Aggregation> aggregationCaptor = ArgumentCaptor.forClass(Aggregation.class);
    verify(mongoTemplate).aggregate(aggregationCaptor.capture(), eq("product"), eq(Product.class));

    Aggregation usedAggregation = aggregationCaptor.getValue();
    assertNotNull(usedAggregation);
  }

  @Test
  void testFindByFilter_SortByBatchEnergyClass() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    Pageable pageable = PageRequest.of(0, 10, Sort.by("energyClass").descending());

    AggregationResults<Product> aggregationResults = mock(AggregationResults.class);
    when(aggregationResults.getMappedResults()).thenReturn(List.of(Product.builder().build()));

    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(aggregationResults);

    List<Product> result = productSpecificRepository.findByFilter(criteria, pageable);

    assertNotNull(result);
    assertEquals(1, result.size());

    ArgumentCaptor<Aggregation> aggregationCaptor = ArgumentCaptor.forClass(Aggregation.class);
    verify(mongoTemplate).aggregate(aggregationCaptor.capture(), eq("product"), eq(Product.class));

    Aggregation usedAggregation = aggregationCaptor.getValue();
    assertNotNull(usedAggregation);
  }


  @Test
  void testFindByFilter_SortByOtherField() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    Pageable pageable = PageRequest.of(0, 10, Sort.by("registrationDate").ascending());

    when(mongoTemplate.find(any(Query.class), eq(Product.class)))
      .thenReturn(List.of(Product.builder().build()));

    productSpecificRepository.findByFilter(criteria, pageable);

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).find(queryCaptor.capture(), eq(Product.class));
    Query queryUsed = queryCaptor.getValue();

    Sort sort = (Sort) ReflectionTestUtils.getField(queryUsed, "sort");
    assertNotNull(sort);

    List<Sort.Order> orders = sort.toList();
    assertEquals(1, orders.size());
    assertEquals("registrationDate", orders.get(0).getProperty());
    assertTrue(orders.get(0).isAscending());
  }

}
