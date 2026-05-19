package it.gov.pagopa.register.service.validator;

import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.mapper.product.ProductMapperStrategy;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.initiative.CategoryExternalCheck;
import it.gov.pagopa.register.model.initiative.ExternalCheckTemplate;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.service.validator.external.system.check.ExternalCheckExecutor;
import it.gov.pagopa.register.service.validator.external.system.check.ExternalCheckResult;
import it.gov.pagopa.register.service.validator.product.ExternalContext;
import it.gov.pagopa.register.service.validator.product.ValidationService;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private ProductMapperStrategy mapper;

  @Mock
  private ExternalCheckExecutor externalCheckExecutor;

  private ValidationService validationService;

  @BeforeEach
  void setUp() {
    Map<String, ProductMapperStrategy> mapperByCategory = Map.of("TEST", mapper);

    validationService = new ValidationService(
        productRepository,
        mapperByCategory,
        externalCheckExecutor
    );
  }
  @Test
  void shouldCallValidateInternal_whenValidateRecordsIsInvoked() {

    CSVRecord csvRecord = mock(CSVRecord.class);

    CategoryConfig categoryConfig = mock(CategoryConfig.class);
    when(categoryConfig.getProductMapper()).thenReturn("TEST");
    when(categoryConfig.getExternalChecks()).thenReturn(Collections.emptyList());

    InitiativeConfig initiativeConfig = mock(InitiativeConfig.class);

    when(mapper.extractBusinessKey(csvRecord, categoryConfig)).thenReturn("KEY");

    when(productRepository.findByGtinCodeAndInitiativeId(any(), any()))
      .thenReturn(Optional.empty());

    when(mapper.mapToProduct(
      any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(new Product());

    // Act
    ProductValidationResult result = validationService.validateRecords(
      List.of(csvRecord),
      "cat",
      "org",
      "INIT",
      "file",
      List.of(),
      "orgName",
      initiativeConfig,
      categoryConfig,
      List.of()
    );


    assertNotNull(result);
  }


  @Test
  void shouldReturnEmptyMap_whenNoExternalChecksConfigured() {

    CSVRecord csvRecord = mock(CSVRecord.class);

    CategoryConfig categoryConfig = mock(CategoryConfig.class);
    when(categoryConfig.getExternalChecks()).thenReturn(List.of());

    ExternalContext context = new ExternalContext(
        mock(InitiativeConfig.class),
        categoryConfig,
        externalCheckExecutor,
        "TEST"
    );

    Map<String, Object> result =
        validationService.performExternalChecks(csvRecord, context);

    assertTrue(result.isEmpty());
  }


  @Test
  void shouldAggregateExternalData_whenExternalChecksAreValid() {

    CSVRecord csvRecord = mock(CSVRecord.class);

    CategoryExternalCheck check = mock(CategoryExternalCheck.class);
    when(check.getKey()).thenReturn("CHECK");
    when(check.getParameters()).thenReturn(Map.of());

    CategoryConfig categoryConfig = mock(CategoryConfig.class);
    when(categoryConfig.getExternalChecks()).thenReturn(List.of(check));

    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);

    InitiativeConfig initiativeConfig = mock(InitiativeConfig.class);
    when(initiativeConfig.getExternalCheckTemplates())
        .thenReturn(Map.of("CHECK", template));

    ExternalCheckResult result = mock(ExternalCheckResult.class);
    when(result.isValid()).thenReturn(true);
    when(result.getExternalData()).thenReturn(Map.of("k", "v"));

    when(externalCheckExecutor.execute(any(), any(), any(), any()))
        .thenReturn(result);

    ExternalContext context = new ExternalContext(
        initiativeConfig,
        categoryConfig,
        externalCheckExecutor,
        "TEST"
    );

    Map<String, Object> output =
        validationService.performExternalChecks(csvRecord, context);

    assertEquals(1, output.size());
    assertEquals("v", output.get("k"));
  }

  @Test
  void shouldReturnNull_whenExternalCheckFails() {

    CSVRecord csvRecord = mock(CSVRecord.class);

    CategoryExternalCheck check = mock(CategoryExternalCheck.class);
    when(check.getKey()).thenReturn("CHECK");
    when(check.getParameters()).thenReturn(Map.of());

    CategoryConfig categoryConfig = mock(CategoryConfig.class);
    when(categoryConfig.getExternalChecks()).thenReturn(List.of(check));

    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);

    InitiativeConfig initiativeConfig = mock(InitiativeConfig.class);
    when(initiativeConfig.getExternalCheckTemplates())
        .thenReturn(Map.of("CHECK", template));

    ExternalCheckResult result = mock(ExternalCheckResult.class);
    when(result.isValid()).thenReturn(false);

    when(externalCheckExecutor.execute(any(), any(), any(), any()))
        .thenReturn(result);

    ExternalContext context = new ExternalContext(
        initiativeConfig,
        categoryConfig,
        externalCheckExecutor,
        "TEST"
    );

    Map<String, Object> output =
        validationService.performExternalChecks(csvRecord, context);

    assertNull(output);
  }

  @Test
  void shouldThrowException_whenMapperIsNull() {
    CategoryConfig categoryConfig = mock(CategoryConfig.class);
    when(categoryConfig.getProductMapper()).thenReturn("UNKNOWN");

    assertThrows(IllegalStateException.class, () ->
      validationService.validateRecords(
        Collections.emptyList(),
        "cat",
        "org",
        "init",
        "file",
        Collections.emptyList(),
        "orgName",
        mock(InitiativeConfig.class),
        categoryConfig,
        Collections.emptyList()
      )
    );
  }

  @Test
  void shouldSetInvalid_whenDbCheckFails() {
    CSVRecord csvRecord = mock(CSVRecord.class);

    CategoryConfig categoryConfig = mock(CategoryConfig.class);
    when(categoryConfig.getProductMapper()).thenReturn("TEST");

    when(mapper.extractBusinessKey(any(), any())).thenReturn("KEY");

    Product existing = new Product();
    existing.setOrganizationId("OTHER");

    when(productRepository.findByGtinCodeAndInitiativeId(any(), any()))
      .thenReturn(Optional.of(existing));

    ProductValidationResult result = validationService.validateRecords(
      List.of(csvRecord),
      "cat",
      "ORG", // diverso → dbCheck = false
      "init",
      "file",
      List.of(),
      "orgName",
      mock(InitiativeConfig.class),
      categoryConfig,
      List.of("VALID")
    );

    assertEquals(1, result.getInvalidRecords().size());
  }


  @Test
  void shouldNotDetectDuplicate_whenSingleRecord() {
    CSVRecord csvRecord = mock(CSVRecord.class);

    CategoryConfig categoryConfig = mock(CategoryConfig.class);
    when(categoryConfig.getProductMapper()).thenReturn("TEST");
    when(categoryConfig.getExternalChecks()).thenReturn(List.of());

    when(mapper.extractBusinessKey(any(), any())).thenReturn("KEY");

    when(productRepository.findByGtinCodeAndInitiativeId(any(), any()))
      .thenReturn(Optional.empty());

    when(mapper.mapToProduct(any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(new Product());

    ProductValidationResult result = validationService.validateRecords(
      List.of(csvRecord),
      "cat",
      "org",
      "init",
      "file",
      List.of(),
      "orgName",
      mock(InitiativeConfig.class),
      categoryConfig,
      List.of()
    );

    assertEquals(1, result.getValidRecords().size());
  }

  @Test
  void shouldInvalidate_whenExternalChecksReturnNull() {
    CSVRecord csvRecord = mock(CSVRecord.class);

    CategoryExternalCheck check = mock(CategoryExternalCheck.class);
    when(check.getKey()).thenReturn("CHECK");

    CategoryConfig categoryConfig = mock(CategoryConfig.class);
    when(categoryConfig.getProductMapper()).thenReturn("TEST");
    when(categoryConfig.getExternalChecks()).thenReturn(List.of(check));

    when(externalCheckExecutor.execute(any(), any(), any(), any()))
      .thenReturn(mock(ExternalCheckResult.class));

    ExternalCheckResult result = mock(ExternalCheckResult.class);
    when(result.isValid()).thenReturn(false);

    when(externalCheckExecutor.execute(any(), any(), any(), any()))
      .thenReturn(result);

    ProductValidationResult res = validationService.validateRecords(
      List.of(csvRecord),
      "cat",
      "org",
      "init",
      "file",
      List.of(),
      "orgName",
      mock(InitiativeConfig.class),
      categoryConfig,
      List.of()
    );

    assertEquals(1, res.getInvalidRecords().size());
  }
}
