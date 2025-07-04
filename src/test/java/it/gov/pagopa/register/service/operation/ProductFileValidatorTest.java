package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.config.ProductFileValidationConfig;
import it.gov.pagopa.register.dto.operation.ValidationResultDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
  ProductFileValidator.class,
  ProductFileValidationConfig.class,

})
@TestPropertySource(properties = "product-file-validation.maxRows=100")
class ProductFileValidatorTest {

  @Autowired
  ProductFileValidator productFileValidator;

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

}
