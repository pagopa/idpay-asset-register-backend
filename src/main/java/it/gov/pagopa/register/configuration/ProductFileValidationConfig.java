package it.gov.pagopa.register.configuration;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class ProductFileValidationConfig {

  @Value("${product-file-validation.maxRows}")
  private int maxRows;
  @Value("${product-file-validation.maxSize}")
  private int maxSize;

}
