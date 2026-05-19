package it.gov.pagopa.register.service.validator;

import it.gov.pagopa.register.configuration.ProductFileValidationConfig;
import it.gov.pagopa.register.configuration.InitiativeConfigMap;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.initiative.CsvTemplate;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import it.gov.pagopa.register.model.initiative.ValidationRule;
import it.gov.pagopa.register.dto.operation.ValidationResultDTO;
import it.gov.pagopa.register.service.validator.rule.RuleDispatcher;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.UploadKeyConstant.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductFileValidatorServiceTest {

  private static final String INITIATIVE_ID = "687f8a176a5c92458819922a";
  private static final String CATEGORY = "COOKINGHOBS";
  private static final String HEADER = "Codice GTIN/EAN";

  @Mock
  private ProductFileValidationConfig validationConfig;
  @Mock
  private InitiativeConfigMap initiativeConfigMap;
  @Mock
  private RuleDispatcher ruleDispatcher;
  @Mock
  private RuleExecutor ruleExecutor;

  private ProductFileValidatorService productFileValidator;

  @BeforeEach
  void setUp() {
    productFileValidator = new ProductFileValidatorService(validationConfig, initiativeConfigMap, ruleDispatcher);
    lenient().when(validationConfig.getMaxSize()).thenReturn(1_000);
    lenient().when(validationConfig.getMaxRows()).thenReturn(100);
  }

  @Test
  void validateFile_FileTypeError() throws IOException {
    MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/csv", "test content".getBytes());

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals(EXTENSION_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_EmptyFileTypeError() throws IOException {
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals(EMPTY_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_SizeFileTypeError() throws IOException {
    when(validationConfig.getMaxSize()).thenReturn(1);
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "too-big".getBytes());

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals(MAX_SIZE_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_InitiativeConfigError() throws IOException {
    MockMultipartFile file = csv("test.csv", HEADER + "\n12345");
    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(null);

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals(INITIATIVE_CONFIG_ERROR, result.getErrorKey());
  }

  @Test
  void validateFile_UnknownCategoryError() throws IOException {
    MockMultipartFile file = csv("test.csv", HEADER + "\n12345");
    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(initiativeConfig());

    ValidationResultDTO result = productFileValidator.validateFile(file, "UNKNOWN", INITIATIVE_ID, "organization");

    assertEquals(UNKNOWN_CATEGORY_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_HeaderFileError() throws IOException {
    MockMultipartFile file = csv("test.csv", "wrong\n12345");
    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(initiativeConfig());

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals(HEADER_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_NoRowError() throws IOException {
    MockMultipartFile file = csv("test.csv", HEADER);
    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(initiativeConfig());

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals(EMPTY_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_MaxRowFileError() throws IOException {
    when(validationConfig.getMaxRows()).thenReturn(0);
    MockMultipartFile file = csv("test.csv", HEADER + "\n12345");
    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(initiativeConfig());

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals(MAX_ROW_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_Ok() throws IOException {
    MockMultipartFile file = csv("valid.csv", HEADER + "\n12345");
    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(initiativeConfig());

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals("OK", result.getStatus());
    assertEquals(1, result.getRecords().size());
  }

  @Test
  void validateRecords_WithInvalidDataErrors() {
    CSVRecord csvRecord = org.mockito.Mockito.mock(CSVRecord.class);
    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(initiativeConfig());
    when(ruleDispatcher.resolve("REGEX")).thenReturn(ruleExecutor);
    when(ruleExecutor.evaluate(any(), any())).thenReturn(false);

    ValidationResultDTO result = productFileValidator.validateRecords(List.of(csvRecord), CATEGORY, INITIATIVE_ID);

    assertEquals("KO", result.getStatus());
    assertFalse(result.getInvalidRecords().isEmpty());
    assertTrue(result.getErrorMessages().get(csvRecord).contains("Piano cottura"));
  }

  @Test
  void validateRecords_WithValidDataReturnsOk() {
    CSVRecord csvRecord = org.mockito.Mockito.mock(CSVRecord.class);
    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(initiativeConfig());
    when(ruleDispatcher.resolve("REGEX")).thenReturn(ruleExecutor);
    when(ruleExecutor.evaluate(any(), any())).thenReturn(true);

    ValidationResultDTO result = productFileValidator.validateRecords(List.of(csvRecord), CATEGORY, INITIATIVE_ID);

    assertEquals("OK", result.getStatus());
    assertNull(result.getInvalidRecords());
  }

  @Test
  void validateFile_BlankFileNameError() throws IOException {
    MockMultipartFile file = new MockMultipartFile("file", "", "text/csv", "test content".getBytes());

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals("KO", result.getStatus());
    assertEquals(EMPTY_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_CsvTemplateNotFoundError() throws IOException {
    MockMultipartFile file = csv("test.csv", HEADER + "\n12345");

    InitiativeConfig config = new InitiativeConfig();
    CategoryConfig categoryConfig = new CategoryConfig("MISSING_TEMPLATE", HEADER, List.of(), "EPREL");
    config.setCategories(Map.of(CATEGORY, categoryConfig));
    config.setCsvTemplates(Map.of());

    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(config);

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals("KO", result.getStatus());
    assertEquals(INITIATIVE_CONFIG_ERROR, result.getErrorKey());
  }

  @Test
  void validateFile_CsvTemplateHeadersEmptyError() throws IOException {
    MockMultipartFile file = csv("test.csv", HEADER + "\n12345");

    InitiativeConfig config = new InitiativeConfig();
    CategoryConfig categoryConfig = new CategoryConfig("TEMPLATE", HEADER, List.of(), "EPREL");
    config.setCategories(Map.of(CATEGORY, categoryConfig));
    config.setCsvTemplates(Map.of("TEMPLATE", new CsvTemplate(null, List.of())));

    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(config);

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals("KO", result.getStatus());
    assertEquals(INITIATIVE_CONFIG_ERROR, result.getErrorKey());
  }

  @Test
  void validateFile_CsvTemplateRulesEmptyError() throws IOException {
    MockMultipartFile file = csv("test.csv", HEADER + "\n12345");

    InitiativeConfig config = new InitiativeConfig();
    CategoryConfig categoryConfig = new CategoryConfig("TEMPLATE", HEADER, List.of(), "EPREL");
    config.setCategories(Map.of(CATEGORY, categoryConfig));
    config.setCsvTemplates(Map.of("TEMPLATE", new CsvTemplate(List.of(HEADER), null)));

    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(config);

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals("KO", result.getStatus());
    assertEquals(MAX_ROW_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_InitiativeCategoriesEmptyError() throws IOException {
    MockMultipartFile file = csv("test.csv", HEADER + "\n12345");

    InitiativeConfig config = new InitiativeConfig();
    config.setCategories(Map.of());

    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(config);

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals("KO", result.getStatus());
    assertEquals(INITIATIVE_CONFIG_ERROR, result.getErrorKey());
  }

  @Test
  void validateFile_CategoryConfigOrTemplateReferenceNullError() throws IOException {
    MockMultipartFile file = csv("test.csv", HEADER + "\n12345");

    InitiativeConfig config = new InitiativeConfig();
    CategoryConfig categoryConfig = new CategoryConfig(null, HEADER, List.of(), "EPREL");
    config.setCategories(new HashMap<>());
    config.getCategories().put(CATEGORY, null);

    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(config);

    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");

    assertEquals("KO", result.getStatus());
    assertEquals(INITIATIVE_CONFIG_ERROR, result.getErrorKey());
  }

  @Test
  void validateFile_CsvTemplateRulesIsEmpty() throws IOException {
    MockMultipartFile file = csv("test.csv", HEADER + "\n12345");
    InitiativeConfig config = new InitiativeConfig();
    CategoryConfig categoryConfig = new CategoryConfig("TEMPLATE", HEADER, List.of(), "EPREL");
    config.setCategories(Map.of(CATEGORY, categoryConfig));
    config.setCsvTemplates(Map.of("TEMPLATE", new CsvTemplate(List.of(HEADER), List.of())));
    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(config);
    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");
    assertEquals("KO", result.getStatus());
    assertEquals(MAX_ROW_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_InitiativeCategoriesIsNull() throws IOException {
    MockMultipartFile file = csv("test.csv", HEADER + "\n12345");
    InitiativeConfig config = new InitiativeConfig();
    config.setCategories(null);
    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(config);
    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");
    assertEquals("KO", result.getStatus());
    assertEquals(INITIATIVE_CONFIG_ERROR, result.getErrorKey());
  }

  @Test
  void validateFile_CategoryConfigCsvTemplateIsNull() throws IOException {
    MockMultipartFile file = csv("test.csv", HEADER + "\n12345");
    InitiativeConfig config = new InitiativeConfig();
    CategoryConfig categoryConfig = new CategoryConfig(null, HEADER, List.of(), "EPREL");
    config.setCategories(Map.of(CATEGORY, categoryConfig));
    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(config);
    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");
    assertEquals("KO", result.getStatus());
    assertEquals(INITIATIVE_CONFIG_ERROR, result.getErrorKey());
  }

  @Test
  void validateFile_CsvTemplateHeadersIsEmpty() throws IOException {
    MockMultipartFile file = csv("test.csv", HEADER + "\n12345");
    InitiativeConfig config = new InitiativeConfig();
    CategoryConfig categoryConfig = new CategoryConfig("TEMPLATE", HEADER, List.of(), "EPREL");
    config.setCategories(Map.of(CATEGORY, categoryConfig));
    config.setCsvTemplates(Map.of("TEMPLATE", new CsvTemplate(List.of(), List.of())));
    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(config);
    ValidationResultDTO result = productFileValidator.validateFile(file, CATEGORY, INITIATIVE_ID, "organization");
    assertEquals("KO", result.getStatus());
    assertEquals(INITIATIVE_CONFIG_ERROR, result.getErrorKey());
  }

  private MockMultipartFile csv(String filename, String content) {
    return new MockMultipartFile("file", filename, "text/csv", content.getBytes());
  }

  private InitiativeConfig initiativeConfig() {
    InitiativeConfig config = new InitiativeConfig();
    CategoryConfig categoryConfig = new CategoryConfig("TEMPLATE", HEADER, List.of(), "EPREL");
    ValidationRule rule = ValidationRule.builder()
      .key("REGEX")
      .field(HEADER)
      .value("\\d+")
      .errorKey("ERROR_CATEGORY_PRODUCTS")
      .build();
    config.setCategories(Map.of(CATEGORY, categoryConfig));
    config.setCsvTemplates(Map.of("TEMPLATE", new CsvTemplate(List.of(HEADER), List.of(rule))));
    return config;
  }
}
