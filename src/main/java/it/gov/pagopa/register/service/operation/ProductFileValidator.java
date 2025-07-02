package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.config.ProductFileValidationConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ProductFileValidator {

  private final ProductFileValidationConfig validationConfig;

  public void validateFile(MultipartFile file, String category) {



  }

}
