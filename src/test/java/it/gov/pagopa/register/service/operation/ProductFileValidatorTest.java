package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.config.ProductFileValidationConfig;
import it.gov.pagopa.register.constants.AssetRegisterConstant;
import it.gov.pagopa.register.dto.operation.ValidationResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
  ProductFileValidator.class,
  ProductFileValidationConfig.class
})
@TestPropertySource(properties = "product-file-validation.maxRows=100")
class ProductFileValidatorTest {

  @Autowired
  ProductFileValidator productFileValidator;
  ProductFileValidationConfig validationConfig;

  @BeforeEach
  void setUp() {
    validationConfig = Mockito.mock(ProductFileValidationConfig.class);
    productFileValidator = new ProductFileValidator(validationConfig);
  }

  @Test
  void validateFile_FileTypeError(){
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "test.csv",
      "text/csv",
      "test content".getBytes());
    String category = "Test";
    List<String> headers = List.of("Test");
    int rowCount = 100;
    ValidationResultDTO result = productFileValidator.validateFile(file,category,headers,rowCount);
    System.out.println(result.getErrorKey());
    assertNotNull(result);
  }

  @Test
  void validateFile_UnknownCategoryError() {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "test.csv",
      "text/csv",
      "test content".getBytes()
    );
    String category = "UnknownCategory";
    List<String> headers = List.of("Test");
    int rowCount = 100;

    ValidationResultDTO result = productFileValidator.validateFile(file, category, headers, rowCount);
    System.out.println(result.getErrorKey());
    assertNotNull(result);
    assertEquals(AssetRegisterConstant.UploadKeyConstant.UNKNOWN_CATEGORY_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_HeaderFileError() {
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

    // Passo un header sbagliato apposta
    List<String> headers = List.of("WrongHeader");
    int rowCount = 100;

    // Act
    ValidationResultDTO result = productFileValidator.validateFile(file, category, headers, rowCount);

    // Assert
    System.out.println(result.getErrorKey());
    assertNotNull(result);
    assertEquals(AssetRegisterConstant.UploadKeyConstant.HEADER_FILE_ERROR_KEY, result.getErrorKey());
  }

  @Test
  void validateFile_EmptyFileError() {
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "test.csv",
      "text/csv",
      "test content".getBytes()
    );

    String category = "COOKINGHOBS";
    LinkedHashMap<String, ColumnValidationRule> mockSchema = new LinkedHashMap<>();
    mockSchema.put("Codice GTIN/EAN", new ColumnValidationRule((v, z) -> true, "Error"));

    Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = new HashMap<>();
    schemas.put(category.toLowerCase(), mockSchema);

    when(validationConfig.getSchemas()).thenReturn(schemas);
    when(validationConfig.getMaxRows()).thenReturn(1000);

    List<String> headers = new ArrayList<>(mockSchema.keySet());

    int rowCount = 0;

    ValidationResultDTO result = productFileValidator.validateFile(file, category, headers, rowCount);

    assertNotNull(result);
    assertEquals(AssetRegisterConstant.UploadKeyConstant.EMPTY_FILE_ERROR_KEY, result.getErrorKey());
  }


  @Test
  void validateFile_MaxRowFileError() {

    MockMultipartFile file = new MockMultipartFile(
      "file",
      "test.csv",
      "text/csv",
      "test content".getBytes()
    );

    String category = "REFRIGERATINGAPPL";

    LinkedHashMap<String, ColumnValidationRule> mockSchema = new LinkedHashMap<>();
    mockSchema.put("Test", new ColumnValidationRule((v, z) -> true, "Error"));

    Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = new HashMap<>();
    schemas.put(category.toLowerCase(), mockSchema);

    when(validationConfig.getSchemas()).thenReturn(schemas);
    when(validationConfig.getMaxRows()).thenReturn(100); // Limite massimo fittizio

    List<String> headers = new ArrayList<>(mockSchema.keySet());
    int rowCount = 101;

    ValidationResultDTO result = productFileValidator.validateFile(file, category, headers, rowCount);

    System.out.println(result.getErrorKey());
    assertNotNull(result);
    assertEquals(AssetRegisterConstant.UploadKeyConstant.MAX_ROW_FILE_ERROR_KEY, result.getErrorKey());
  }
}
