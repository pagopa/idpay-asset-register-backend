package it.gov.pagopa.register.config;


import it.gov.pagopa.register.service.operation.ColumnValidationRule;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "product-file-validation")
@Data
public class ProductFileValidationConfig {

  @Value("max-rows")
  private int maxRows;

  private Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas;

}
