package it.gov.pagopa.register.repository;

import it.gov.pagopa.register.dto.operation.EmailProductDTO;
import it.gov.pagopa.register.dto.operation.ProductCriteriaDTO;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductSpecificRepositoryImpl;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSpecificRepositoryTest {

  @Mock
  private MongoTemplate mongoTemplate;

  @InjectMocks
  private ProductSpecificRepositoryImpl repository;

  @BeforeEach
  void setUp() {
    repository = new ProductSpecificRepositoryImpl(mongoTemplate);
  }

  // ======================
  // findByFilter - NO SORT => usa find(Query.with(pageable))
  // ======================
  @Test
  void testFindByFilter_noSort_shouldUseFindWithPageable() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    Pageable pageable = PageRequest.of(0, 10);

    Product product = Product.builder().organizationId("org1").build();

    when(mongoTemplate.find(any(Query.class), eq(Product.class)))
      .thenReturn(List.of(product));

    List<Product> results = repository.findByFilter(criteria, pageable);

    // verify è stato usato find e NON aggregate
    ArgumentCaptor<Query> qCaptor = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).find(qCaptor.capture(), eq(Product.class));
    verify(mongoTemplate, never()).aggregate(any(Aggregation.class), anyString(), eq(Product.class));

    Query used = qCaptor.getValue();
    assertEquals(criteria.getCriteriaObject(), used.getQueryObject());
    assertEquals(10, used.getLimit());
    assertEquals(0, used.getSkip());

    assertEquals(1, results.size());
    assertEquals("org1", results.get(0).getOrganizationId());
  }

  // ==========================================
  // findByFilter - SORT case-insensitive su gtinCode => aggregation con $toLower su $_id
  // ==========================================
  @Test
  void testFindByFilter_ciSortOnGtinCode_shouldAggregateWithToLowerOn_Id() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("gtinCode"))); // FE field

    AggregationResults<Product> empty = new AggregationResults<>(List.of(), new Document());
    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(empty);

    repository.findByFilter(criteria, pageable);

    ArgumentCaptor<Aggregation> aggCaptor = ArgumentCaptor.forClass(Aggregation.class);
    verify(mongoTemplate).aggregate(aggCaptor.capture(), eq("product"), eq(Product.class));
    verify(mongoTemplate, never()).find(any(Query.class), eq(Product.class));

    String pipeline = aggCaptor.getValue().toString();
    // deve contenere: $match, addFields con _id_lower via $toLower, $sort su _id_lower, poi $skip/$limit
    assertTrue(pipeline.contains("$match"));
    assertTrue(pipeline.contains("\"_id_lower\""));
    assertTrue(pipeline.contains("$toLower"));
    assertTrue(pipeline.contains("\"$_id\""));
    assertTrue(pipeline.contains("$sort"));
    assertTrue(pipeline.contains("\"_id_lower\""));
    assertTrue(pipeline.contains("$skip"));
    assertTrue(pipeline.contains("$limit"));
  }

  // ======================================================
  // findByFilter - SORT case-insensitive su productName => aggregation con $toLower su productName_lower
  // ======================================================
  @Test
  void testFindByFilter_ciSortOnProductName_shouldAggregateWithToLower() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Order.desc(Product.Fields.productName)));

    AggregationResults<Product> empty = new AggregationResults<>(List.of(), new Document());
    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(empty);

    repository.findByFilter(criteria, pageable);

    ArgumentCaptor<Aggregation> aggCaptor = ArgumentCaptor.forClass(Aggregation.class);
    verify(mongoTemplate).aggregate(aggCaptor.capture(), eq("product"), eq(Product.class));

    String pipeline = aggCaptor.getValue().toString();
    assertTrue(pipeline.contains("$addFields"));
    assertTrue(pipeline.contains("productName_lower"));
    assertTrue(pipeline.contains("$toLower"));
    assertTrue(pipeline.contains("\"$productName\""));
    assertTrue(pipeline.contains("$sort"));
    assertTrue(pipeline.contains("\"productName_lower\""));
    assertTrue(pipeline.contains("$skip"));
    assertTrue(pipeline.contains("$limit"));
    assertTrue(pipeline.contains("$project")); // i campi *_lower vengono rimossi
  }

  // ============================================================
  // findByFilter - SORT generico (non CI) per registrationDate => aggregation con sort/skip/limit
  // ============================================================
  @Test
  void testFindByFilter_genericSort_shouldAggregateAndSortBeforePaginate() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    Pageable pageable = PageRequest.of(1, 20, Sort.by(Sort.Order.desc("registrationDate"))); // non CI

    AggregationResults<Product> empty = new AggregationResults<>(List.of(), new Document());
    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(empty);

    repository.findByFilter(criteria, pageable);

    ArgumentCaptor<Aggregation> aggCaptor = ArgumentCaptor.forClass(Aggregation.class);
    verify(mongoTemplate).aggregate(aggCaptor.capture(), eq("product"), eq(Product.class));
    verify(mongoTemplate, never()).find(any(Query.class), eq(Product.class));

    String pipeline = aggCaptor.getValue().toString();

    // Ordine corretto: $match -> $sort -> $skip -> $limit
    int iMatch = pipeline.indexOf("$match");
    int iSort  = pipeline.indexOf("$sort");
    int iSkip  = pipeline.indexOf("$skip");
    int iLimit = pipeline.indexOf("$limit");

    assertTrue(iMatch >= 0 && iSort > iMatch && iSkip > iSort && iLimit > iSkip);
    assertTrue(pipeline.contains("\"registrationDate\""));
  }

  // ============================================================
  // Ramo special: sort per batchName/category (categoryIt + productFileId)
  // ============================================================
  @Test
  void testResolveSort_shouldApplyCustomSortForBatchName() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("batchName")));
    Criteria criteria = Criteria.where("organizationId").is("org1");

    AggregationResults<Product> mockResults = new AggregationResults<>(List.of(), new Document());
    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(mockResults);

    repository.findByFilter(criteria, pageable);

    ArgumentCaptor<Aggregation> aggregationCaptor = ArgumentCaptor.forClass(Aggregation.class);
    verify(mongoTemplate).aggregate(aggregationCaptor.capture(), eq("product"), eq(Product.class));

    Aggregation usedAggregation = aggregationCaptor.getValue();
    String pipelineString = usedAggregation.toString();

    assertTrue(pipelineString.contains("categoryIt"));
    assertTrue(pipelineString.contains("productFileId"));
  }

  // ============================================================
  // Ramo special: sort per energyClass (energyRank)
  // ============================================================
  @Test
  void testFindByFilter_energyClassAggregation_shouldSortByEnergyRank() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("energyClass")));
    Criteria criteria = new Criteria();

    AggregationResults<Product> mockResults = new AggregationResults<>(List.of(), new Document());
    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(mockResults);

    repository.findByFilter(criteria, pageable);

    ArgumentCaptor<Aggregation> aggregationCaptor = ArgumentCaptor.forClass(Aggregation.class);
    verify(mongoTemplate).aggregate(aggregationCaptor.capture(), eq("product"), eq(Product.class));

    String pipeline = aggregationCaptor.getValue().toString();
    assertTrue(pipeline.contains("energyRank"));
    assertTrue(pipeline.contains("$sort"));
    assertTrue(pipeline.contains("\"energyRank\""));
    assertTrue(pipeline.contains("$skip"));
    assertTrue(pipeline.contains("$limit"));
  }

  // ======================
  // getCriteria
  // ======================
  @Test
  void testGetCriteria_shouldBuildCorrectly() {
    Criteria criteria = repository.getCriteria(
      ProductCriteriaDTO.builder()
        .organizationId("org1")
        .category("cat")
        .productFileId("productFileId")
        .eprelCode("eprel")
        .gtinCode("gtin")
        .productName("productName")
        .fullProductName("fullProductName")
        .brand("brand")
        .model("model")
        .status("status")
        .build()
    );

    assertNotNull(criteria);
    assertEquals("org1", criteria.getCriteriaObject().get("organizationId"));
    assertEquals("cat", criteria.getCriteriaObject().get("category"));
    assertEquals("productFileId", criteria.getCriteriaObject().get("productFileId"));
    // regex case-insensitive: eprel/productName/fullProductName/brand/model/gtin su FIELD_ID
    String regexEprel = criteria.getCriteriaObject().get("eprelCode").toString();
    assertTrue(regexEprel.contains("eprel"));
  }

  // ======================
  // getCount
  // ======================
  @Test
  void testGetCount_shouldReturnCorrectCount() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(5L);

    Long count = repository.getCount(criteria);

    assertEquals(5L, count);
  }

  // ======================
  // retrieveDistinctProductFileIdsBasedOnRole
  // ======================
  @Test
  void testFindDistinctProductFileIdAndCategoryByOrganizationId_shouldReturnResults() {
    Product mockProduct = Product.builder()
      .productFileId("file123")
      .category("categoryABC")
      .build();

    @SuppressWarnings("unchecked")
    AggregationResults<Product> aggregationResults = mock(AggregationResults.class);
    when(aggregationResults.getMappedResults()).thenReturn(List.of(mockProduct));

    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(aggregationResults);

    List<Product> result = repository.retrieveDistinctProductFileIdsBasedOnRole("org1", null, "operatore");

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("file123", result.get(0).getProductFileId());
    assertEquals("categoryABC", result.get(0).getCategory());

    verify(mongoTemplate, times(1)).aggregate(any(Aggregation.class), eq("product"), eq(Product.class));
  }

  // ======================
  // getProductNamesGroupedByEmail
  // ======================
  @Test
  void testGetProductNamesGroupedByEmail_shouldReturnGroupedResults() {
    EmailProductDTO dto = EmailProductDTO.builder()
      .id("user@example.com")
      .productNames(List.of("Product A", "Product B"))
      .build();

    AggregationResults<EmailProductDTO> aggregationResults = mock(AggregationResults.class);
    when(aggregationResults.getMappedResults()).thenReturn(List.of(dto));
    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(EmailProductDTO.class)))
      .thenReturn(aggregationResults);

    List<EmailProductDTO> results =
      repository.getProductNamesGroupedByEmail(List.of("gtin1", "gtin2"));

    assertEquals(1, results.size());
    assertEquals("user@example.com", results.get(0).getId());
    assertTrue(results.get(0).getProductNames().containsAll(List.of("Product A", "Product B")));
  }

  // ======================
  // findByIds
  // ======================
  @Test
  void testFindByIds_shouldReturnProducts() {
    Product p = Product.builder().gtinCode("gtin1").build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(p));

    List<Product> res = repository.findByIds(List.of("gtin1", "gtin2"));

    assertEquals(1, res.size());
    verify(mongoTemplate).find(any(Query.class), eq(Product.class));
  }

  // ======================
  // findUpdatableProducts (stati validi)
  // ======================
  @Test
  void testFindUpdatableProducts_APPROVED_invitaliaAdmin() {
    Product product = Product.builder().gtinCode("gtin1").organizationId("org1").status("UPLOAD").build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(product));

    List<Product> results = repository.findUpdatableProducts(
      List.of("gtin1"),
      ProductStatus.WAIT_APPROVED,
      ProductStatus.APPROVED,
      UserRole.INVITALIA_ADMIN.getRole()
    );

    assertEquals(1, results.size());
    assertEquals("gtin1", results.get(0).getGtinCode());
  }

  @Test
  void testFindUpdatableProducts_UPLOADED_invitaliaAdmin() {
    Product product = Product.builder().gtinCode("gtin1").organizationId("org1").status("APPROVED").build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(product));

    List<Product> results = repository.findUpdatableProducts(
      List.of("gtin1"),
      ProductStatus.WAIT_APPROVED,
      ProductStatus.UPLOADED,
      UserRole.INVITALIA_ADMIN.getRole()
    );

    assertEquals(1, results.size());
    assertEquals("gtin1", results.get(0).getGtinCode());
  }

  @Test
  void testFindUpdatableProducts_SUPERVISED_invitalia() {
    Product product = Product.builder().gtinCode("gtin1").organizationId("org1").status("UPLOAD").build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(product));

    List<Product> results = repository.findUpdatableProducts(
      List.of("gtin1"),
      ProductStatus.UPLOADED,
      ProductStatus.SUPERVISED,
      "invitalia"
    );

    assertEquals(1, results.size());
    assertEquals("gtin1", results.get(0).getGtinCode());
  }

  @Test
  void testFindUpdatableProducts_WAIT_APPROVED_invitalia() {
    Product product = Product.builder().gtinCode("gtin1").organizationId("org1").status("UPLOAD").build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(product));

    List<Product> results = repository.findUpdatableProducts(
      List.of("gtin1"),
      ProductStatus.UPLOADED,
      ProductStatus.WAIT_APPROVED,
      "invitalia"
    );

    assertEquals(1, results.size());
    assertEquals("gtin1", results.get(0).getGtinCode());
  }

  // ======================
  // findUpdatableProducts (transizione NON permessa) => lista vuota, non chiama find
  // ======================
  @Test
  void testFindUpdatableProducts_invalidTransition_shouldReturnEmptyAndNotQueryDb() {
    List<Product> results = repository.findUpdatableProducts(
      List.of("gtin1"),
      ProductStatus.APPROVED,              // stato corrente NON tra quelli permessi
      ProductStatus.UPLOADED,
      UserRole.INVITALIA_ADMIN.getRole()
    );

    assertTrue(results.isEmpty());
    verify(mongoTemplate, never()).find(any(Query.class), eq(Product.class));
  }
}
