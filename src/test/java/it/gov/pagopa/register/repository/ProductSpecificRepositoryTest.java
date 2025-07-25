package it.gov.pagopa.register.repository;

import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductSpecificRepositoryImpl;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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
public class ProductSpecificRepositoryTest {
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
    when(mongoTemplate.find(expectedQuery, Product.class)).thenReturn(List.of(product));

    List<Product> results = repository.findByFilter(criteria, pageable);

    assertEquals(1, results.size());
    assertEquals("org1", results.get(0).getOrganizationId());
  }

  @Test
  void testGetCriteria_shouldBuildCorrectly() {
    Criteria criteria = repository.getCriteria("org1", "cat", "code", "file", "eprel", "gtin");

    assertNotNull(criteria);
    assertEquals("org1", criteria.getCriteriaObject().get("organizationId"));
    assertEquals("cat", criteria.getCriteriaObject().get("category"));
    assertEquals("code", criteria.getCriteriaObject().get("productCode"));
    assertEquals("file", criteria.getCriteriaObject().get("productFileId"));
  }

  @Test
  void testGetCount_shouldReturnCorrectCount() {
    Criteria criteria = Criteria.where("organizationId").is("org1");
    when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(5L);

    Long count = repository.getCount(criteria);

    assertEquals(5L, count);
  }

  @Test
  void testFindDistinctProductFileIdAndCategoryByOrganizationId_shouldReturnResults() {
    // Arrange
    Product mockProduct = Product.builder()
      .productFileId("file123")
      .category("categoryABC")
      .build();

    @SuppressWarnings("unchecked")
    AggregationResults<Product> aggregationResults = mock(AggregationResults.class);
    when(aggregationResults.getMappedResults()).thenReturn(List.of(mockProduct));

    when(mongoTemplate.aggregate(
      any(Aggregation.class),
      eq("product"), // Deve essere esattamente lo stesso usato nella tua impl
      eq(Product.class))
    ).thenReturn(aggregationResults);

    // Act
    List<Product> result = repository.findDistinctProductFileIdAndCategoryByOrganizationId("org1");

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("file123", result.get(0).getProductFileId());
    assertEquals("categoryABC", result.get(0).getCategory());

    // Verifica che il metodo aggregate sia stato chiamato
    verify(mongoTemplate, times(1)).aggregate(any(Aggregation.class), eq("product"), eq(Product.class));
  }


  @Test
  void testFindByIdsAndOrganizationId_shouldReturnMatchingProducts() {
    Product product = Product.builder().gtinCode("gtin1").organizationId("org1").build();
    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(product));

    List<Product> results = repository.findByIdsAndOrganizationId(List.of("gtin1"), "org1");

    assertEquals(1, results.size());
    assertEquals("gtin1", results.get(0).getGtinCode());
    assertEquals("org1", results.get(0).getOrganizationId());
  }

  @Test
  void testResolveSort_shouldApplyCustomSortForBatchName() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("batchName")));
    Criteria criteria = Criteria.where("organizationId").is("org1");

    when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of());

    repository.findByFilter(criteria, pageable);

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).find(queryCaptor.capture(), eq(Product.class));

    Query usedQuery = queryCaptor.getValue();
    Document sortDocument = usedQuery.getSortObject();

    // Verifica indiretta del nuovo sort applicato
    assertTrue(sortDocument.containsKey("category"));
    assertTrue(sortDocument.containsKey("productFileId"));
    assertEquals(1, sortDocument.get("category")); // 1 = ASC
    assertEquals(1, sortDocument.get("productFileId"));
  }

}
