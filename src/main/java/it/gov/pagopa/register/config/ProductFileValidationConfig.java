package it.gov.pagopa.register.config;


import it.gov.pagopa.register.service.operation.ColumnValidationRule;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static it.gov.pagopa.register.constants.AssetRegisterConstant.*;

@Component
@Data
public class ProductFileValidationConfig {


  @Value("${product-file-validation.maxRows}")
  private int maxRows;

  private Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = initialize();
  private static Map<String, LinkedHashMap<String, ColumnValidationRule>> initialize(){
    LinkedHashMap<String, ColumnValidationRule> cookingHobsSchema = new LinkedHashMap<>();
    cookingHobsSchema.put(
      CODICE_GTIN_EAN,
      new ColumnValidationRule((v,z)-> v != null && v.matches(CODICE_GTIN_EAN_REGEX), ERROR_GTIN_EAN));
    cookingHobsSchema.put(
      CODICE_PRODOTTO,
      new ColumnValidationRule((v,z) -> v != null && v.matches(CODICE_PRODOTTO_REGEX), ERROR_CODICE_PRODOTTO));
    cookingHobsSchema.put(
      CATEGORIA,
      new ColumnValidationRule(String::equals, ERROR_CATEGORIA_COOKINGHOBS));
    cookingHobsSchema.put(PAESE_DI_PRODUZIONE,
      new ColumnValidationRule((v,z) -> Arrays.asList(Locale.getISOCountries()).contains(v), ERROR_PAESE_DI_PRODUZIONE));
    cookingHobsSchema.put(
      MARCA,
      new ColumnValidationRule((v,z) -> v != null && v.matches(MARCA_REGEX), ERROR_MARCA));
    cookingHobsSchema.put(
      MODELLO,
      new ColumnValidationRule((v,z) -> v != null && v.matches(MODELLO_REGEX), ERROR_MODELLO));

    LinkedHashMap<String, ColumnValidationRule> eprelSchema = new LinkedHashMap<>();
    eprelSchema.put(
      CODICE_EPREL,
      new ColumnValidationRule((v,z) -> v != null && v.matches(CODICE_EPREL_REGEX), ERROR_CODICE_EPREL));
    eprelSchema.put(
      CODICE_GTIN_EAN,
      new ColumnValidationRule((v,z) -> v != null && v.matches(CODICE_GTIN_EAN_REGEX), ERROR_GTIN_EAN));
    eprelSchema.put(
      CODICE_PRODOTTO,
      new ColumnValidationRule((v,z) -> v != null && v.matches(CODICE_PRODOTTO_REGEX), ERROR_CODICE_PRODOTTO));
    eprelSchema.put(
      CATEGORIA,
      new ColumnValidationRule(String::equals, ERROR_CATEGORIA_PRODOTTI));
    eprelSchema.put(
      PAESE_DI_PRODUZIONE,
      new ColumnValidationRule((v,z) -> Arrays.asList(Locale.getISOCountries()).contains(v), ERROR_PAESE_DI_PRODUZIONE));

    Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = new LinkedHashMap<>();
    schemas.put("cookinghobs", cookingHobsSchema);
    schemas.put("eprel", eprelSchema);
    return  schemas;
  }


}


