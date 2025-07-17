package it.gov.pagopa.register.configuration;


import it.gov.pagopa.register.dto.utils.ColumnValidationRule;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;

@Component
@Data
public class ProductFileValidationConfig {

  @Value("${product-file-validation.maxRows}")
  private int maxRows;

  private Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = initialize();

  private static Map<String, LinkedHashMap<String, ColumnValidationRule>> initialize() {
    LinkedHashMap<String, ColumnValidationRule> cookingHobsSchema = new LinkedHashMap<>();
    cookingHobsSchema.put(CODE_GTIN_EAN, CsvValidationRules.GTIN_EAN_RULE);
    cookingHobsSchema.put(CODE_PRODUCT, CsvValidationRules.CODE_PRODUCT_RULE);
    cookingHobsSchema.put(CATEGORY, CsvValidationRules.CATEGOY_COOKINGHOBS_RULE);
    cookingHobsSchema.put(COUNTRY_OF_PRODUCTION, CsvValidationRules.COUNTRY_OF_PRODUCTION_RULE);
    cookingHobsSchema.put(BRAND, CsvValidationRules.BRAND_RULE);
    cookingHobsSchema.put(MODEL, CsvValidationRules.MODEL_RULE);

    LinkedHashMap<String, ColumnValidationRule> eprelSchema = new LinkedHashMap<>();
    eprelSchema.put(CODE_EPREL, CsvValidationRules.CODE_EPREL_RULE);
    eprelSchema.put(CODE_GTIN_EAN, CsvValidationRules.GTIN_EAN_RULE);
    eprelSchema.put(CODE_PRODUCT, CsvValidationRules.CODE_PRODUCT_RULE);
    eprelSchema.put(CATEGORY, CsvValidationRules.CATEGOY_PRODUCTS_RULE);
    eprelSchema.put(COUNTRY_OF_PRODUCTION, CsvValidationRules.COUNTRY_OF_PRODUCTION_RULE);

    Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = new LinkedHashMap<>();
    schemas.put("cookinghobs", cookingHobsSchema);
    schemas.put("eprel", eprelSchema);
    return schemas;
  }
}
