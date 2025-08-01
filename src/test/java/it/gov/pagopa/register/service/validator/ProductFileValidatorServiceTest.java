package it.gov.pagopa.register.service.validator;

import it.gov.pagopa.register.configuration.ProductFileValidationConfig;
import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.dto.operation.ValidationResultDTO;
import it.gov.pagopa.register.dto.utils.ColumnValidationRule;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
  ProductFileValidatorService.class,
})
class ProductFileValidatorServiceTest {

  @Autowired
  ProductFileValidatorService productFileValidator;

  @MockitoBean
  ProductFileValidationConfig validationConfig;

  @BeforeEach
  void setUp() {
    validationConfig = Mockito.mock(ProductFileValidationConfig.class);
    productFileValidator = new ProductFileValidatorService(validationConfig);
    when(validationConfig.getMaxSize()).thenReturn(22);
    when(validationConfig.getMaxRows()).thenReturn(100);
  }

  @Test
  void validateFile_FileTypeError() throws IOException {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "test.txt",
      "text/csv",
      "test content".getBytes());
    String category = "Test";
    ValidationResultDTO result = productFileValidator.validateFile(file,category);
    assertNotNull(result);
    assertEquals(AssetRegisterConstants.UploadKeyConstant.EXTENSION_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_EmpyFileTypeError() throws IOException {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "test.csv",
      "text/csv",
      "".getBytes());
    String category = "Test";
    ValidationResultDTO result = productFileValidator.validateFile(file,category);
    assertNotNull(result);
    assertEquals(AssetRegisterConstants.UploadKeyConstant.EMPTY_FILE_ERROR_KEY, result.getErrorKey());
  }
  @Test
  void validateFile_SizeFileTypeError() throws IOException {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "test.csv",
      "text/csv",
      "Codice GTIN/EAN\\n1234567".getBytes());
    String category = "Test";
    ValidationResultDTO result = productFileValidator.validateFile(file,category);
    assertNotNull(result);
    assertEquals(AssetRegisterConstants.UploadKeyConstant.MAX_SIZE_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_UnknownCategoryError() throws IOException {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "test.csv",
      "text/csv",
      "test".getBytes()
    );
    String category = "UnknownCategory";

    ValidationResultDTO result = productFileValidator.validateFile(file, category);
    System.out.println(result.getErrorKey());
    assertNotNull(result);
    assertEquals(AssetRegisterConstants.UploadKeyConstant.UNKNOWN_CATEGORY_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_HeaderFileError() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "test.csv",
      "text/csv",
      "test content".getBytes()
    );
    String category = "COOKINGHOBS";
    LinkedHashMap<String, ColumnValidationRule> mockSchema = new LinkedHashMap<>();
    mockSchema.put("Codice GTIN/EAN", new ColumnValidationRule((v, z) -> true, "Error"));
    mockSchema.put("Codice Prodotto", new ColumnValidationRule((v, z) -> true, "Error"));

    Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = new HashMap<>();
    schemas.put(category.toLowerCase(), mockSchema);

    when(validationConfig.getSchemas()).thenReturn(schemas);
    when(validationConfig.getMaxRows()).thenReturn(100);

    // Act
    ValidationResultDTO result = productFileValidator.validateFile(file, category);

    // Assert
    System.out.println(result.getErrorKey());
    assertNotNull(result);
    assertEquals(AssetRegisterConstants.UploadKeyConstant.HEADER_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_NoRowError() throws IOException {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "test.csv",
      "text/csv",
      "Codice GTIN/EAN".getBytes()
    );

    String category = "COOKINGHOBS";
    LinkedHashMap<String, ColumnValidationRule> mockSchema = new LinkedHashMap<>();
    mockSchema.put("Codice GTIN/EAN", new ColumnValidationRule((v, z) -> true, "Error"));

    Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = new HashMap<>();
    schemas.put(category.toLowerCase(), mockSchema);

    when(validationConfig.getSchemas()).thenReturn(schemas);
    when(validationConfig.getMaxRows()).thenReturn(1000);


    ValidationResultDTO result = productFileValidator.validateFile(file, category);

    assertNotNull(result);
    assertEquals(AssetRegisterConstants.UploadKeyConstant.EMPTY_FILE_ERROR_KEY, result.getErrorKey());
  }


  @Test
  void validateFile_MaxRowFileError() throws IOException {

    MockMultipartFile file = new MockMultipartFile(
      "file",
      "test.csv",
      "text/csv",
      "Codice GTIN/EAN\n12345".getBytes()
    );

    String category = "COOKINGHOBS";

    LinkedHashMap<String, ColumnValidationRule> mockSchema = new LinkedHashMap<>();
    mockSchema.put("Codice GTIN/EAN", new ColumnValidationRule((v, z) -> true, "Error"));

    Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = new HashMap<>();
    schemas.put(category.toLowerCase(), mockSchema);

    when(validationConfig.getSchemas()).thenReturn(schemas);
    when(validationConfig.getMaxRows()).thenReturn(0);

    ValidationResultDTO result = productFileValidator.validateFile(file, category);

    System.out.println(result.getErrorKey());
    assertNotNull(result);
    assertEquals(AssetRegisterConstants.UploadKeyConstant.MAX_ROW_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_InvalidFileExtensionError() throws IOException {
    MockMultipartFile file = new MockMultipartFile(
      "file", "file.txt", "text/plain", "invalid content".getBytes()
    );

    String category = "COOKINGHOBS";


    ValidationResultDTO result = productFileValidator.validateFile(file, category);

    assertNotNull(result);
    assertEquals(AssetRegisterConstants.UploadKeyConstant.EXTENSION_FILE_ERROR_KEY, result.getErrorKey());
  }


  @Test
  void validateFile_Ok() throws IOException {
    MockMultipartFile file = new MockMultipartFile(
      "file", "valid.csv", "text/csv",
      "Codice GTIN/EAN\n12345".getBytes()
    );

    String category = "COOKINGHOBS";

    LinkedHashMap<String, ColumnValidationRule> mockSchema = new LinkedHashMap<>();
    mockSchema.put("Codice GTIN/EAN", new ColumnValidationRule((v, z) -> true, "Error"));

    Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = new HashMap<>();
    schemas.put(category.toLowerCase(), mockSchema);

    when(validationConfig.getSchemas()).thenReturn(schemas);
    when(validationConfig.getMaxRows()).thenReturn(100);


    ValidationResultDTO result = productFileValidator.validateFile(file, category);

    assertNotNull(result);
    assertEquals("OK", result.getStatus());
  }


  @Test
  void validateRecords_WithInvalidDataErrors() {
    String category = "COOKINGHOBS";

    LinkedHashMap<String, ColumnValidationRule> mockSchema = new LinkedHashMap<>();
    mockSchema.put("Codice GTIN/EAN", new ColumnValidationRule(
      (value, cat) -> value != null && value.matches("\\d{13}"), "Invalid GTIN"
    ));

    Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = new HashMap<>();
    schemas.put(category.toLowerCase(), mockSchema);
    when(validationConfig.getSchemas()).thenReturn(schemas);

    List<String> headers = List.of("Codice GTIN/EAN");

    CSVRecord csvRecord = Mockito.mock(CSVRecord.class);
    when(csvRecord.get("Codice GTIN/EAN")).thenReturn("123ABC");

    List<CSVRecord> records = List.of(csvRecord);

    ValidationResultDTO result = productFileValidator.validateRecords(records, headers, category);

    assertNotNull(result);
    assertFalse(result.getInvalidRecords().isEmpty());
    assertTrue(result.getErrorMessages().get(csvRecord).contains("Invalid GTIN"));
  }


  @Test
  void validateRecords_NoRulesFoundException() {
    String category = "UNKNOWN";
    when(validationConfig.getSchemas()).thenReturn(Collections.emptyMap());

    List<CSVRecord> records = List.of();
    List<String> headers = List.of();

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
      productFileValidator.validateRecords(records, headers, category)
    );

    assertEquals("No validation rules found for category: " + category, exception.getMessage());
  }


}
