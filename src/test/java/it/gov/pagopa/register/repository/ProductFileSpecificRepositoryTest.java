package it.gov.pagopa.register.repository;

import it.gov.pagopa.register.dto.operation.ProductCriteriaDTO;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductSpecificRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
      ProductCriteriaDTO.builder()
        .organizationId("org1")
        .category("cat")
        .productFileId("fileId")
        .eprelCode("eprel")
        .gtinCode("gtin")
        .productName("productName")
        .brand("brand")
        .model("model")
        .status("status")
        .build()
    );

    assertNotNull(criteria);
    assertTrue(criteria.getCriteriaObject().containsKey("organizationId"));
    assertTrue(criteria.getCriteriaObject().containsKey("category"));
    assertTrue(criteria.getCriteriaObject().containsKey("productFileId"));
    assertTrue(criteria.getCriteriaObject().containsKey("eprelCode"));
    assertTrue(criteria.getCriteriaObject().containsKey("_id"));
    assertTrue(criteria.getCriteriaObject().containsKey("productName"));
    assertTrue(criteria.getCriteriaObject().containsKey("brand"));
    assertTrue(criteria.getCriteriaObject().containsKey("model"));
    assertTrue(criteria.getCriteriaObject().containsKey("status"));
  }

  @Test
  void testGetCriteria_OnlyOrgIdPresent() {
    Criteria criteria = productSpecificRepository.getCriteria(
      ProductCriteriaDTO.builder()
        .organizationId("org1")
        .build()
    );

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

    List<Product> result = productSpecificRepository.retrieveDistinctProductFileIdsBasedOnRole(orgId, null, UserRole.OPERATORE.getRole());

    assertEquals(1, result.size());
    assertEquals("file123", result.getFirst().getProductFileId());
    assertEquals("catA", result.getFirst().getCategory());

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
  void testFindByFilter_SortByOtherField_shouldUseAggregationGenericSort() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    Pageable pageable = PageRequest.of(0, 10, Sort.by("registrationDate").ascending());

    AggregationResults<Product> empty = new AggregationResults<>(List.of(), new org.bson.Document());
    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(empty);

    productSpecificRepository.findByFilter(criteria, pageable);

    verify(mongoTemplate, never()).find(any(Query.class), eq(Product.class));

    ArgumentCaptor<Aggregation> aggCaptor = ArgumentCaptor.forClass(Aggregation.class);
    verify(mongoTemplate).aggregate(aggCaptor.capture(), eq("product"), eq(Product.class));

    String pipeline = aggCaptor.getValue().toString();

    int iMatch = pipeline.indexOf("$match");
    int iSort  = pipeline.indexOf("$sort");
    int iSkip  = pipeline.indexOf("$skip");
    int iLimit = pipeline.indexOf("$limit");

    assertTrue(iMatch >= 0, "manca $match");
    assertTrue(iSort > iMatch, "manca/ordine errato di $sort");
    assertTrue(iSkip > iSort, "manca/ordine errato di $skip");
    assertTrue(iLimit > iSkip, "manca/ordine errato di $limit");

    assertTrue(pipeline.contains("\"registrationDate\""), "il sort non è su registrationDate");
    assertTrue(pipeline.contains("\"registrationDate\" : 1") || pipeline.toLowerCase().contains("asc"),
      "il sort non è ascendente");
  }


  @Test
  @DisplayName("findByIds - ritorna i prodotti richiesti")
  void testFindByIds_ReturnsProducts() {
    Product p1 = Product.builder().gtinCode("GTIN-1").status("UPLOADED").build();
    Product p2 = Product.builder().gtinCode("GTIN-2").status("REJECTED").build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(p1, p2));

    List<Product> result = productSpecificRepository.findByIds(List.of("GTIN-1", "GTIN-2"));

    assertEquals(2, result.size());
    Set<String> gtins = result.stream().map(Product::getGtinCode).collect(Collectors.toSet());
    assertTrue(gtins.containsAll(List.of("GTIN-1", "GTIN-2")));
  }

  @Test
  @DisplayName("findByIds - ritorna lista vuota se non trova nulla")
  void testFindByIds_ReturnsEmptyWhenNoneFound() {
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of());

    List<Product> result = productSpecificRepository.findByIds(List.of("MISSING"));

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("findByIds - ignora duplicati in input")
  void testFindByIds_IgnoresDuplicateIds() {
    Product p2 = Product.builder().gtinCode("GTIN-2").status("REJECTED").build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(p2));

    List<Product> result = productSpecificRepository.findByIds(List.of("GTIN-2", "GTIN-2"));

    assertEquals(1, result.size());
    assertEquals("GTIN-2", result.getFirst().getGtinCode());
  }

  @Nested
  class EdgeCases {
    @Test
    void testFindByIds_EmptyInput() {
      List<Product> result = productSpecificRepository.findByIds(List.of());
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }

    @Test
    void testFindByIds_MixedExistingAndMissing() {
      Product p1 = Product.builder().gtinCode("GTIN-1").status("UPLOADED").build();
      when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(p1));

      List<Product> result = productSpecificRepository.findByIds(List.of("GTIN-1", "MISSING"));
      assertEquals(1, result.size());
      assertEquals("GTIN-1", result.getFirst().getGtinCode());
    }
  }
}
