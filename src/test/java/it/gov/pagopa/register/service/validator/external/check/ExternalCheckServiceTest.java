package it.gov.pagopa.register.service.validator.external.check;


import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.mapper.product.ProductMapperStrategy;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.initiative.CategoryExternalCheck;
import it.gov.pagopa.register.model.initiative.ExternalCheckTemplate;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.gov.pagopa.register.utils.ValidationUtils.dbCheck;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(
  classes = {
    ExternalCheckService.class,
  }
)
class ExternalCheckServiceTest {

  @Autowired
  private ExternalCheckService service;

  @MockitoBean
  private ProductRepository productRepository;

  @MockitoBean
  private ExternalCheckExecutor externalCheckExecutor;

  @MockitoBean
  private Map<String, ProductMapperStrategy> mapperByCategory;

  private ProductMapperStrategy mapper;

  @BeforeEach
  void setUp() {

    mapper = mock(ProductMapperStrategy.class);

    when(mapperByCategory.get("mapperKey")).thenReturn(mapper);

    when(externalCheckExecutor.execute(any(), any(), any(), any()))
      .thenReturn(ExternalCheckResult.ok(Map.of()));

    when(mapper.extractBusinessKey(any(), any())).thenReturn("GTIN1");
    when(mapper.mapToProduct(any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(new Product());
  }

  @Test
  void shouldValidateValidRecord() {

    when(productRepository.findByGtinCodeAndInitiativeId(any(), any()))
      .thenReturn(Optional.empty());

    ProductValidationResult result = service.validateRecords(
      List.of(mock(CSVRecord.class)),
      "TEST_CATEGORY",
      "ORG",
      "INIT",
      "FILE",
      List.of("h"),
      "ORG_NAME",
      buildInitiativeConfig(),
      buildCategoryConfig(),
      List.of()
    );

    assertEquals(1, result.getValidRecords().size());
    assertEquals(0, result.getInvalidRecords().size());
  }

  @Test
  void shouldMarkDuplicateAsInvalid() {

    when(productRepository.findByGtinCodeAndInitiativeId(any(), any()))
      .thenReturn(Optional.empty());

    ProductValidationResult result = service.validateRecords(
      List.of(mock(CSVRecord.class), mock(CSVRecord.class)),
      "TEST_CATEGORY",
      "ORG",
      "INIT",
      "FILE",
      List.of("h"),
      "ORG_NAME",
      buildInitiativeConfig(),
      buildCategoryConfig(),
      List.of()
    );

    assertFalse(result.getInvalidRecords().isEmpty());
  }

  @Test
  void shouldFailWhenExternalCheckFails() {

    when(externalCheckExecutor.execute(any(), any(), any(), any()))
      .thenReturn(ExternalCheckResult.ko("ERR"));

    when(productRepository.findByGtinCodeAndInitiativeId(any(), any()))
      .thenReturn(Optional.empty());

    ProductValidationResult result = service.validateRecords(
      List.of(mock(CSVRecord.class)),
      "TEST_CATEGORY",
      "ORG",
      "INIT",
      "FILE",
      List.of("h"),
      "ORG_NAME",
      buildInitiativeConfig(),
      buildCategoryConfig(),
      List.of()
    );

    assertEquals(1, result.getInvalidRecords().size());
  }

  @Test
  void shouldMarkInvalidWhenDbCheckFails() {

    when(productRepository.findByGtinCodeAndInitiativeId(any(), any()))
      .thenReturn(Optional.empty());

    try (MockedStatic<it.gov.pagopa.register.utils.ValidationUtils> mocked =
           mockStatic(it.gov.pagopa.register.utils.ValidationUtils.class)) {

      mocked.when(() -> dbCheck(any(), any(), any(), any(), any(), any()))
        .thenReturn(false);

      ProductValidationResult result = service.validateRecords(
        List.of(mock(CSVRecord.class)),
        "TEST_CATEGORY",
        "ORG",
        "INIT",
        "FILE",
        List.of("h"),
        "ORG_NAME",
        buildInitiativeConfig(),
        buildCategoryConfig(),
        List.of()
      );

      assertEquals(0, result.getInvalidRecords().size());
    }
  }

  private InitiativeConfig buildInitiativeConfig() {
    InitiativeConfig config = mock(InitiativeConfig.class);

    CategoryConfig categoryItem = mock(CategoryConfig.class);
    when(categoryItem.getProductMapper()).thenReturn("mapperKey");

    when(config.getCategories())
      .thenReturn(Map.of("TEST_CATEGORY", categoryItem));

    when(config.getExternalCheckTemplates())
      .thenReturn(Map.of("CHECK1", mock(ExternalCheckTemplate.class)));

    return config;
  }

  private CategoryConfig buildCategoryConfig() {
    CategoryConfig categoryConfig = mock(CategoryConfig.class);

    CategoryExternalCheck check = mock(CategoryExternalCheck.class);
    when(check.getKey()).thenReturn("CHECK1");
    when(check.getParameters()).thenReturn(Map.of());

    when(categoryConfig.getExternalChecks())
      .thenReturn(List.of(check));

    return categoryConfig;
  }

  @Test
  void shouldCopyDatabaseFieldsWhenProductExistsInDb() {
    Product dbProduct = new Product();
    dbProduct.setFormalMotivation("MOCK_MOTIVATION");

    it.gov.pagopa.register.model.operation.StatusChangeEvent mockEvent =
      mock(it.gov.pagopa.register.model.operation.StatusChangeEvent.class);

    ArrayList<it.gov.pagopa.register.model.operation.StatusChangeEvent> history = new ArrayList<>();
    history.add(mockEvent);
    dbProduct.setStatusChangeChronology(history);

    when(productRepository.findByGtinCodeAndInitiativeId("GTIN1", "INIT"))
      .thenReturn(Optional.of(dbProduct));

    Product mappedProduct = new Product();
    when(mapper.mapToProduct(any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(mappedProduct);

    try (MockedStatic<it.gov.pagopa.register.utils.ValidationUtils> mocked =
           mockStatic(it.gov.pagopa.register.utils.ValidationUtils.class)) {

      mocked.when(() -> dbCheck(any(), any(), any(), any(), any(), any()))
        .thenReturn(true);

      ProductValidationResult result = service.validateRecords(
        List.of(mock(CSVRecord.class)),
        "TEST_CATEGORY",
        "ORG",
        "INIT",
        "FILE",
        List.of("h"),
        "ORG_NAME",
        buildInitiativeConfig(),
        buildCategoryConfig(),
        List.of()
      );

      assertEquals(1, result.getValidRecords().size());
      Product finalProduct = result.getValidRecords().get("GTIN1");

      assertEquals("MOCK_MOTIVATION", finalProduct.getFormalMotivation());
      assertEquals(1, finalProduct.getStatusChangeChronology().size());
    }
  }
}
