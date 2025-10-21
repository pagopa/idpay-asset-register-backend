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

  @Test
  void testFindByFilter_shouldReturnProducts() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    Pageable pageable = PageRequest.of(0, 10);
    Query expectedQuery = Query.query(criteria).with(pageable);

    Product product = Product.builder().organizationId("org1").build();

    when(mongoTemplate.find(any(Query.class), eq(Product.class)))
      .thenReturn(List.of(product));

    List<Product> results = repository.findByFilter(criteria, pageable);

    ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).find(captor.capture(), eq(Product.class));
    Query used = captor.getValue();

    assertEquals(expectedQuery.getQueryObject(), used.getQueryObject());
    assertEquals(expectedQuery.getLimit(), used.getLimit());
    assertEquals(expectedQuery.getSkip(), used.getSkip());

    assertTrue(used.getCollation().isPresent());

    assertEquals(1, results.size());
    assertEquals("org1", results.get(0).getOrganizationId());
  }


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
        .brand("brand")
        .model("model")
        .status("status")
        .build()
    );

    assertNotNull(criteria);
    assertEquals("org1", criteria.getCriteriaObject().get("organizationId"));
    assertEquals("cat", criteria.getCriteriaObject().get("category"));
    assertEquals("productFileId", criteria.getCriteriaObject().get("productFileId"));
  }

  @Test
  void testFindByFilter_energyClassAggregation_shouldApplyCollationAndPipeline() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("energyClass")));

    AggregationResults<Product> mockResults = new AggregationResults<>(List.of(), new Document());
    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(mockResults);

    repository.findByFilter(criteria, pageable);

    ArgumentCaptor<Aggregation> aggregationCaptor = ArgumentCaptor.forClass(Aggregation.class);
    verify(mongoTemplate).aggregate(aggregationCaptor.capture(), eq("product"), eq(Product.class));

    Aggregation used = aggregationCaptor.getValue();

    assertTrue(used.getOptions().getCollation().isPresent());

    String pipeline = used.toString();
    assertTrue(pipeline.contains("energyRank"));
    assertTrue(pipeline.contains("$sort"));
    assertTrue(pipeline.contains("energyRank"));
    assertTrue(pipeline.contains("$skip"));
    assertTrue(pipeline.contains("$limit"));
  }


  @Test
  void testGetCount_shouldReturnCorrectCount() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(5L);

    Long count = repository.getCount(criteria);

    assertEquals(5L, count);
  }

  @Test
  void testFindByFilter_categoryAggregation_shouldApplyCollationAndPipeline() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("category")));

    AggregationResults<Product> mockResults = new AggregationResults<>(List.of(), new Document());
    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(mockResults);

    repository.findByFilter(criteria, pageable);

    ArgumentCaptor<Aggregation> aggregationCaptor = ArgumentCaptor.forClass(Aggregation.class);
    verify(mongoTemplate).aggregate(aggregationCaptor.capture(), eq("product"), eq(Product.class));

    Aggregation used = aggregationCaptor.getValue();

    assertTrue(used.getOptions().getCollation().isPresent());

    String pipeline = used.toString();
    assertTrue(pipeline.contains("categoryIt"));
    assertTrue(pipeline.contains("$sort"));
  }


  @Test
  void testFindDistinctProductFileIdAndCategoryByOrganizationId_shouldReturnResults() {
    Product mockProduct = Product.builder()
      .productFileId("file123")
      .category("categoryABC")
      .build();

    @SuppressWarnings("unchecked")
    AggregationResults<Product> aggregationResults = mock(AggregationResults.class);
    when(aggregationResults.getMappedResults()).thenReturn(List.of(mockProduct));

    when(mongoTemplate.aggregate(
      any(Aggregation.class),
      eq("product"),
      eq(Product.class))
    ).thenReturn(aggregationResults);


    List<Product> result = repository.retrieveDistinctProductFileIdsBasedOnRole("org1",null,"operatore");

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("file123", result.get(0).getProductFileId());
    assertEquals("categoryABC", result.get(0).getCategory());

    verify(mongoTemplate, times(1)).aggregate(any(Aggregation.class), eq("product"), eq(Product.class));
  }

  @Test
  void testFindUpdatableProducts_invalidTransition_returnsEmpty() {
    List<Product> results = repository.findUpdatableProducts(
      List.of("gtin1"),
      ProductStatus.APPROVED,
      ProductStatus.UPLOADED,
      UserRole.INVITALIA_ADMIN.getRole()
    );
    assertTrue(results.isEmpty());
    verify(mongoTemplate, never()).find(any(Query.class), eq(Product.class));
  }

  @Test
  void testGetAllowedInitialStates_invitalia() {
    List<String> states1 = repository.getAllowedInitialStates(ProductStatus.SUPERVISED, UserRole.INVITALIA.getRole());
    assertEquals(List.of(ProductStatus.UPLOADED.name()), states1);

    List<String> states2 = repository.getAllowedInitialStates(ProductStatus.WAIT_APPROVED, UserRole.INVITALIA.getRole());
    assertTrue(states2.contains(ProductStatus.UPLOADED.name()));
    assertTrue(states2.contains(ProductStatus.SUPERVISED.name()));
  }

  @Test
  void testGetAllowedInitialStates_invitaliaAdmin() {
    List<String> states1 = repository.getAllowedInitialStates(ProductStatus.APPROVED, UserRole.INVITALIA_ADMIN.getRole());
    assertEquals(List.of(ProductStatus.WAIT_APPROVED.name()), states1);

    List<String> states2 = repository.getAllowedInitialStates(ProductStatus.UPLOADED, UserRole.INVITALIA_ADMIN.getRole());
    assertEquals(List.of(ProductStatus.WAIT_APPROVED.name()), states2);

    // Stato non gestito -> lista vuota
    List<String> states3 = repository.getAllowedInitialStates(ProductStatus.REJECTED, UserRole.INVITALIA_ADMIN.getRole());
    assertTrue(states3.isEmpty());
  }

  @Test
  void testFindByIds_shouldReturnProducts() {
    Product p = Product.builder().gtinCode("gtin1").build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(p));

    List<Product> res = repository.findByIds(List.of("gtin1", "gtin2"));

    assertEquals(1, res.size());
    verify(mongoTemplate).find(any(Query.class), eq(Product.class));
  }

  @Test
  void testRetrieveDistinctProductFileIdsBasedOnRole_withOrganizationSelected() {
    Product mockProduct = Product.builder()
      .productFileId("file999")
      .category("catX")
      .build();

    @SuppressWarnings("unchecked")
    AggregationResults<Product> aggregationResults = mock(AggregationResults.class);
    when(aggregationResults.getMappedResults()).thenReturn(List.of(mockProduct));

    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(Product.class)))
      .thenReturn(aggregationResults);

    List<Product> res = repository.retrieveDistinctProductFileIdsBasedOnRole("org1", "orgSelected", "invitalia");
    assertEquals(1, res.size());
    assertEquals("file999", res.get(0).getProductFileId());
    assertEquals("catX", res.get(0).getCategory());
  }


  @Test
  void testFindUpdatableProducts_APPROVED_invitaliaAdmin() {
    Product product = Product.builder().gtinCode("gtin1").organizationId("org1").status("UPLOAD").build();

    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(product));

    List<Product> results = repository.findUpdatableProducts(List.of("gtin1"),ProductStatus.WAIT_APPROVED, ProductStatus.APPROVED, UserRole.INVITALIA_ADMIN.getRole());

    assertEquals(1, results.size());
    assertEquals("gtin1", results.get(0).getGtinCode());
  }

  @Test
  void testFindUpdatableProducts_UPLOAD_invitaliaAdmin() {
    Product product = Product.builder().gtinCode("gtin1").organizationId("org1").status("APPROVED").build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(product));

    List<Product> results = repository.findUpdatableProducts(List.of("gtin1"),ProductStatus.WAIT_APPROVED, ProductStatus.UPLOADED, UserRole.INVITALIA_ADMIN.getRole());

    assertEquals(1, results.size());
    assertEquals("gtin1", results.get(0).getGtinCode());
  }


  @Test
  void testFindUpdatableProducts_SUPERVISIONED_invitalia() {
    Product product = Product.builder().gtinCode("gtin1").organizationId("org1").status("UPLOAD").build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(product));

    List<Product> results = repository.findUpdatableProducts(List.of("gtin1"), ProductStatus.UPLOADED, ProductStatus.SUPERVISED, "invitalia");

    assertEquals(1, results.size());
    assertEquals("gtin1", results.get(0).getGtinCode());
  }

  @Test
  void testFindUpdatableProducts_WAIT_APPROVED_invitalia() {
    Product product = Product.builder().gtinCode("gtin1").organizationId("org1").status("UPLOAD").build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(product));

    List<Product> results = repository.findUpdatableProducts(List.of("gtin1"),  ProductStatus.UPLOADED,ProductStatus.WAIT_APPROVED, "invitalia");

    assertEquals(1, results.size());
    assertEquals("gtin1", results.get(0).getGtinCode());
  }



  @Test
  void testGetProductNamesGroupedByEmail_shouldReturnGroupedResults() {

    EmailProductDTO dto = EmailProductDTO
      .builder()
      .id("user@example.com")
      .productNames(List.of("Product A", "Product B"))
      .build();

    AggregationResults<EmailProductDTO> aggregationResults = mock(AggregationResults.class);
    when(aggregationResults.getMappedResults()).thenReturn(List.of(dto));
    when(mongoTemplate.aggregate(any(Aggregation.class), eq("product"), eq(EmailProductDTO.class)))
      .thenReturn(aggregationResults);

    List<EmailProductDTO> results = repository.getProductNamesGroupedByEmail(List.of("gtin1", "gtin2"));

    assertEquals(1, results.size());
    assertEquals("user@example.com", results.get(0).getId());
    assertTrue(results.get(0).getProductNames().contains("Product A"));
    assertTrue(results.get(0).getProductNames().contains("Product B"));
  }



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



}
