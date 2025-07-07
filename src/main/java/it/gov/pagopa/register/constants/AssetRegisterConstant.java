package it.gov.pagopa.register.constants;

import it.gov.pagopa.register.service.operation.ColumnValidationRule;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public class AssetRegisterConstant {

  //private constructor to avoid instantiation
  private AssetRegisterConstant(){
  }

  public static final String EPREL_ERROR = "EPREL_ERROR";
  public static final String FORMAL_ERROR = "FORMAL_ERROR";

  // Regex
  public static final String CODE_GTIN_EAN_REGEX = "^[a-zA-Z0-9]{1,14}$";
  public static final String BRAND_REGEX = "^.{1,100}$";
  public static final String MODEL_REGEX = "^.{1,100}$";
  public static final String CODE_PRODUCT_REGEX = "^[a-zA-Z0-9 ]{0,100}$";
  public static final String CODE_EPREL_REGEX = "^\\d+$";

  // CSV HEADER
  public static final String CODE_EPREL = "Codice EPREL";
  public static final String CODE_GTIN_EAN = "Codice GTIN/EAN";
  public static final String CODE_PRODUCT = "Codice Prodotto";
  public static final String CATEGORY = "Categoria";
  public static final String COUNTRY_OF_PRODUCTION = "Paese di Produzione";
  public static final String BRAND = "Marca";
  public static final String MODEL = "Modello";

  // Category
  public static final String WASHINGMACHINES = "WASHINGMACHINES";
  public static final String WASHERDRIERS = "WASHERDRIERS";
  public static final String OVENS = "OVENS";
  public static final String RANGEHOODS = "RANGEHOODS";
  public static final String DISHWASHERS = "DISHWASHERS";
  public static final String TUMBLEDRIERS = "TUMBLEDRIERS";
  public static final String REFRIGERATINGAPPL = "REFRIGERATINGAPPL";
  public static final String COOKINGHOBS = "COOKINGHOBS";

  public static final Set<String> CATEGORIES = Set.of(
    WASHINGMACHINES,
    WASHERDRIERS,
    OVENS,
    RANGEHOODS,
    DISHWASHERS,
    TUMBLEDRIERS,
    REFRIGERATINGAPPL,
    COOKINGHOBS
  );

  // Errors
  public static final String ERROR_GTIN_EAN = "Il Codice GTIN/EAN è obbligatorio e deve essere univoco ed alfanumerico e lungo al massimo 14 caratteri";
  public static final String ERROR_CATEGORY_COOKINGHOBS = "Il campo Categoria è obbligatorio e deve contenere il valore fisso 'COOKINGHOBS'";
  public static final String ERROR_BRAND = "Il campo Marca è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri";
  public static final String ERROR_MODEL = "Il campo Modello è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri";
  public static final String ERROR_CODE_PRODUCT = "Il Codice prodotto non deve contenere caratteri speciali o lettere accentate e deve essere lungo al massimo 100 caratteri";
  public static final String ERROR_COUNTRY_OF_PRODUCTION = "Paese di Produzione non è un ISO 3166 valido";
  public static final String ERROR_CODE_EPREL = "Il Codice EPREL è obbligatorio e deve essere un valore numerico";
  public static final String ERROR_CATEGORY_PRODUCTS = "Il campo Categoria è obbligatorio e deve essere coerente con la categoria selezionata";


  public static final class UploadKeyConstant {
    private UploadKeyConstant(){}
    public static final String EXTENSION_FILE_ERROR_KEY = "product.invalid.file.extension";
    public static final String MAX_ROW_FILE_ERROR_KEY = "product.invalid.file.maxrow";
    public static final String HEADER_FILE_ERROR_KEY = "product.invalid.file.header";
    public static final String REPORT_FORMAL_FILE_ERROR_KEY = "product.invalid.file.report";
    public static final String UNKNOWN_CATEGORY_ERROR_KEY = "product.invalid.file.category";
    public static final String EMPTY_FILE_ERROR_KEY = "product.invalid.file.empty";
  }


  public static final class ValidationRules {
    private ValidationRules() {}
    public static final ColumnValidationRule GTIN_EAN_RULE =
      new ColumnValidationRule((v, z) -> v != null && v.matches(CODE_GTIN_EAN_REGEX), ERROR_GTIN_EAN);

    public static final ColumnValidationRule CODE_PRODUCT_RULE =
      new ColumnValidationRule((v, z) -> v != null && v.matches(CODE_PRODUCT_REGEX), ERROR_CODE_PRODUCT);

    public static final ColumnValidationRule CATEGOY_COOKINGHOBS_RULE =
      new ColumnValidationRule(String::equals, ERROR_CATEGORY_COOKINGHOBS);

    public static final ColumnValidationRule CATEGOY_PRODUCTS_RULE =
      new ColumnValidationRule(String::equals, ERROR_CATEGORY_PRODUCTS);

    public static final ColumnValidationRule COUNTRY_OF_PRODUCTION_RULE =
      new ColumnValidationRule((v, z) -> Arrays.asList(Locale.getISOCountries()).contains(v), ERROR_COUNTRY_OF_PRODUCTION);

    public static final ColumnValidationRule BRAND_RULE =
      new ColumnValidationRule((v, z) -> v != null && v.matches(BRAND_REGEX), ERROR_BRAND);

    public static final ColumnValidationRule MODEL_RULE =
      new ColumnValidationRule((v, z) -> v != null && v.matches(MODEL_REGEX), ERROR_MODEL);

    public static final ColumnValidationRule CODE_EPREL_RULE =
      new ColumnValidationRule((v, z) -> v != null && v.matches(CODE_EPREL_REGEX), ERROR_CODE_EPREL);
  }

}
