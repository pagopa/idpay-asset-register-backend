package it.gov.pagopa.register.config;


import it.gov.pagopa.register.service.operation.ColumnValidationRule;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
@Component
@Data
public class ProductFileValidationConfig {


  private int maxRows;

  private Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = initialize();
  private static Map<String, LinkedHashMap<String, ColumnValidationRule>> initialize(){
    LinkedHashMap<String, ColumnValidationRule> cookingHobsSchema = new LinkedHashMap<>();
    cookingHobsSchema.put("Codice GTIN/EAN",
      new ColumnValidationRule(v -> v != null && v.matches("^[a-zA-Z0-9]{1,14}$"),
        "Il Codice GTIN/EAN è obbligatorio e deve essere univoco ed alfanumerico e lungo al massimo 14 caratteri"));
    cookingHobsSchema.put("Codice Prodotto",
      new ColumnValidationRule(v -> v != null && v.matches("^[a-zA-Z0-9 ]{0,100}$"),
        "Il Codice prodotto non deve contenere caratteri speciali o lettere accentate e deve essere lungo al massimo 100 caratteri"));
    cookingHobsSchema.put("Categoria",
      new ColumnValidationRule(v -> v != null && v.matches("^[A-G]$"),
        "Il campo Categoria è obbligatorio"));
    cookingHobsSchema.put("Paese di Produzione",
      new ColumnValidationRule(v -> Arrays.asList(Locale.getISOCountries()).contains(v),
        "Il Paese di Produzione è obbligatorio e deve essere composto da esattamente 2 caratteri"));
    cookingHobsSchema.put("Marca",
      new ColumnValidationRule(v -> v != null && v.matches("^.{1,100}$"),
        "Il campo Marca è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri"));
    cookingHobsSchema.put("Modello",
      new ColumnValidationRule(v -> v != null && v.matches("^.{1,100}$"),
        "Il campo Modello è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri"));

    LinkedHashMap<String, ColumnValidationRule> eprelSchema = new LinkedHashMap<>();
    eprelSchema.put("Codice EPREL",
      new ColumnValidationRule(v -> v != null && v.matches("^\\d+$"),
        "Il Codice EPREL è obbligatorio e deve essere un valore numerico"));
    eprelSchema.put("Codice GTIN/EAN",
      new ColumnValidationRule(v -> v != null && v.matches("^[a-zA-Z0-9]{1,14}$"),
        "Il Codice GTIN/EAN è obbligatorio e deve essere univoco ed alfanumerico e lungo al massimo 14 caratteri"));
    eprelSchema.put("Codice Prodotto",
      new ColumnValidationRule(v -> v != null && v.matches("^[a-zA-Z0-9 ]{0,100}$"),
        "Il Codice prodotto non deve contenere caratteri speciali o lettere accentate e deve essere lungo al massimo 100 caratteri"));
    eprelSchema.put("Categoria",
      new ColumnValidationRule(v -> v != null && v.matches("^[A-G]$"),
        "Il campo Categoria è obbligatorio"));
    eprelSchema.put("Paese di Produzione",
      new ColumnValidationRule(v -> Arrays.asList(Locale.getISOCountries()).contains(v),
        "Il Paese di Produzione è obbligatorio e deve essere composto da esattamente 2 caratteri"));

    Map<String, LinkedHashMap<String, ColumnValidationRule>> schemas = new LinkedHashMap<>();
    schemas.put("cookinghobs", cookingHobsSchema);
    schemas.put("eprel", eprelSchema);
    return  schemas;
  }


}


