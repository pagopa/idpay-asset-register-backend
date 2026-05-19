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
    validationService = new ValidationService(
      productRepository,
      Map.of("TEST", mapper),
      externalCheckExecutor
    );
  }

  @Test
  void shouldReturnValidRecord_whenNoExternalChecks() {
    CSVRecord csvRecord = mock(CSVRecord.class);

    CategoryConfig category = mock(CategoryConfig.class);
    when(category.getProductMapper()).thenReturn("TEST");
    when(category.getExternalChecks()).thenReturn(List.of());

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
      category,
      List.of("VALID")
    );

    assertEquals(1, result.getValidRecords().size());
  }

  @Test
  void shouldMarkInvalid_whenDbCheckFails() {
    CSVRecord csvRecord = mock(CSVRecord.class);

    CategoryConfig category = mock(CategoryConfig.class);
    when(category.getProductMapper()).thenReturn("TEST");

    when(mapper.extractBusinessKey(any(), any())).thenReturn("KEY");

    Product existing = new Product();
    existing.setOrganizationId("OTHER");

    when(productRepository.findByGtinCodeAndInitiativeId(any(), any()))
      .thenReturn(Optional.of(existing));

    ProductValidationResult result = validationService.validateRecords(
      List.of(csvRecord),
      "cat",
      "ORG",
      "init",
      "file",
      List.of(),
      "orgName",
      mock(InitiativeConfig.class),
      category,
      List.of("VALID")
    );

    assertEquals(1, result.getInvalidRecords().size());
  }

  @Test
  void shouldThrowException_whenMapperIsMissing() {
    CategoryConfig category = mock(CategoryConfig.class);
    when(category.getProductMapper()).thenReturn("UNKNOWN");

    assertThrows(IllegalStateException.class,
      () -> validationService.validateRecords(
        List.of(),
        "cat",
        "org",
        "init",
        "file",
        List.of(),
        "orgName",
        mock(InitiativeConfig.class),
        category,
        List.of()
      )
    );
  }

  @Test
  void shouldNotDetectDuplicate_whenSingleRecord() {
    CSVRecord csvRecord = mock(CSVRecord.class);

    CategoryConfig category = mock(CategoryConfig.class);
    when(category.getProductMapper()).thenReturn("TEST");
    when(category.getExternalChecks()).thenReturn(List.of());

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
      category,
      List.of("VALID")
    );

    assertEquals(1, result.getValidRecords().size());
  }


  @Test
  void shouldReturnOk_whenExternalChecksNotConfigured() {
    CSVRecord csv = mock(CSVRecord.class);

    CategoryConfig category = mock(CategoryConfig.class);
    when(category.getExternalChecks()).thenReturn(List.of());

    ExternalContext context = new ExternalContext(
      mock(InitiativeConfig.class),
      category,
      externalCheckExecutor,
      "TEST"
    );

    ExternalCheckResult result =
      validationService.performExternalChecks(csv, context);

    assertTrue(result.isValid());
    assertTrue(result.getExternalData().isEmpty());
  }

  @Test
  void shouldAggregateExternalData_whenExternalChecksPass() {
    CSVRecord csv = mock(CSVRecord.class);

    CategoryExternalCheck check = mock(CategoryExternalCheck.class);
    when(check.getKey()).thenReturn("CHECK");
    when(check.getParameters()).thenReturn(Map.of());

    CategoryConfig category = mock(CategoryConfig.class);
    when(category.getExternalChecks()).thenReturn(List.of(check));

    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);

    InitiativeConfig initiative = mock(InitiativeConfig.class);
    when(initiative.getExternalCheckTemplates())
      .thenReturn(Map.of("CHECK", template));

    when(externalCheckExecutor.execute(any(), any(), any(), any()))
      .thenReturn(ExternalCheckResult.ok(Map.of("k", "v")));

    ExternalContext context =
      new ExternalContext(initiative, category, externalCheckExecutor, "TEST");

    ExternalCheckResult result =
      validationService.performExternalChecks(csv, context);

    assertTrue(result.isValid());
    assertEquals("v", result.getExternalData().get("k"));
  }

  @Test
  void shouldReturnKo_whenExternalCheckFails() {
    CSVRecord csv = mock(CSVRecord.class);

    CategoryExternalCheck check = mock(CategoryExternalCheck.class);
    when(check.getKey()).thenReturn("CHECK");
    when(check.getParameters()).thenReturn(Map.of());

    CategoryConfig category = mock(CategoryConfig.class);
    when(category.getExternalChecks()).thenReturn(List.of(check));

    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);

    InitiativeConfig initiative = mock(InitiativeConfig.class);
    when(initiative.getExternalCheckTemplates())
      .thenReturn(Map.of("CHECK", template));

    when(externalCheckExecutor.execute(any(), any(), any(), any()))
      .thenReturn(ExternalCheckResult.ko("ERROR"));

    ExternalContext context =
      new ExternalContext(initiative, category, externalCheckExecutor, "TEST");

    ExternalCheckResult result =
      validationService.performExternalChecks(csv, context);

    assertFalse(result.isValid());
    assertEquals("ERROR", result.getErrorMessage());
  }

  @Test
  void shouldInvalidateRecord_whenExternalCheckFailsInValidationFlow() {
    CSVRecord csvRecord = mock(CSVRecord.class);

    CategoryExternalCheck check = mock(CategoryExternalCheck.class);
    when(check.getKey()).thenReturn("CHECK");
    when(check.getParameters()).thenReturn(Map.of());

    CategoryConfig category = mock(CategoryConfig.class);
    when(category.getProductMapper()).thenReturn("TEST");
    when(category.getExternalChecks()).thenReturn(List.of(check));

    when(mapper.extractBusinessKey(any(), any())).thenReturn("KEY");

    Product existing = new Product();
    existing.setOrganizationId("org");
    existing.setStatus("VALID");

    when(productRepository.findByGtinCodeAndInitiativeId(any(), any()))
      .thenReturn(Optional.of(existing));

    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);

    InitiativeConfig initiative = mock(InitiativeConfig.class);
    when(initiative.getExternalCheckTemplates())
      .thenReturn(Map.of("CHECK", template));

    when(externalCheckExecutor.execute(any(), any(), any(), any()))
      .thenReturn(ExternalCheckResult.ko("ERR"));

    ProductValidationResult result = validationService.validateRecords(
      List.of(csvRecord),
      "cat",
      "org",
      "init",
      "file",
      List.of(),
      "orgName",
      initiative,
      category,
      List.of("VALID")
    );

    assertEquals(1, result.getInvalidRecords().size());
    assertFalse(result.getErrorMessages().isEmpty());
  }
}
