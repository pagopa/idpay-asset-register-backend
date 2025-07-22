package it.gov.pagopa.register.constants;

import it.gov.pagopa.register.dto.utils.ColumnValidationRule;
import it.gov.pagopa.register.dto.utils.EprelValidationRule;

import java.util.*;
import java.util.regex.Pattern;

import static it.gov.pagopa.register.utils.EprelUtils.isEnergyClassValid;

public class AssetRegisterConstants {

  //private constructor to avoid instantiation
  private AssetRegisterConstants(){
  }

  public static final String REPORT_PARTIAL_ERROR = "Report/Partial/";
  public static final String REPORT_FORMAL_ERROR = "Report/Formal/";
  public static final String CSV = ".csv";
  public static final Integer CSV_SIZE = 2 * 1024 * 1024;

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
  public static final String TUMBLEDRYERS = "TUMBLEDRYERS";
  public static final String REFRIGERATINGAPPL = "REFRIGERATINGAPPL";
  public static final String COOKINGHOBS = "COOKINGHOBS";
  public static final String WASHINGMACHINES_IT = "Lavatrici";
  public static final String WASHERDRIERS_IT = "Lavasciuga";
  public static final String OVENS_IT = "Forni";
  public static final String RANGEHOODS_IT = "Cappe";
  public static final String DISHWASHERS_IT = "Lavastoviglie";
  public static final String TUMBLEDRYERS_IT = "Asciugatrici";
  public static final String REFRIGERATINGAPPL_IT = "Frigoriferi";
  public static final String COOKINGHOBS_IT = "Piani cottura";
  public static final Set<String> CATEGORIES = Set.of(
    WASHINGMACHINES,
    WASHERDRIERS,
    OVENS,
    RANGEHOODS,
    DISHWASHERS,
    TUMBLEDRYERS,
    REFRIGERATINGAPPL,
    COOKINGHOBS
  );


  public static final Map<String, String> CATEGORIES_TO_IT = Map.of(
    WASHINGMACHINES_IT, WASHINGMACHINES,
    WASHERDRIERS_IT, WASHERDRIERS,
    OVENS_IT, OVENS,
    RANGEHOODS_IT, RANGEHOODS,
    DISHWASHERS_IT, DISHWASHERS,
    TUMBLEDRYERS_IT, TUMBLEDRYERS,
    REFRIGERATINGAPPL_IT, REFRIGERATINGAPPL,
    COOKINGHOBS_IT, COOKINGHOBS
  );

  // Eprel Value

  public static final String ORG_VERIFICATION_STATUS = "orgVerificationStatus";
  public static final String TRADE_MARKER_VERIFICATION_STATUS = "trademarkVerificationStatus";
  public static final String BLOCKED = "blocked";
  public static final String STATUS = "status";
  public static final String PRODUCT_GROUP = "productGroup";
  public static final String ENERGY_CLASS = "energyClass";

  public static final Set<String> EPREL_FIELDS = Set.of(
    ORG_VERIFICATION_STATUS,
    TRADE_MARKER_VERIFICATION_STATUS,
    BLOCKED,
    STATUS,
    PRODUCT_GROUP,
    ENERGY_CLASS
  );


  // Csv Errors
  public static final String ERROR_GTIN_EAN = "Il Codice GTIN/EAN è obbligatorio e deve essere univoco ed alfanumerico e lungo al massimo 14 caratteri";
  public static final String ERROR_CATEGORY_COOKINGHOBS = "Il campo Categoria è obbligatorio e deve contenere il valore fisso 'COOKINGHOBS'";
  public static final String ERROR_BRAND = "Il campo Marca è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri";
  public static final String ERROR_MODEL = "Il campo Modello è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri";
  public static final String ERROR_CODE_PRODUCT = "Il Codice prodotto non deve contenere caratteri speciali o lettere accentate e deve essere lungo al massimo 100 caratteri";
  public static final String ERROR_COUNTRY_OF_PRODUCTION = "Paese di Produzione non è un ISO 3166 valido";
  public static final String ERROR_CODE_EPREL = "Il Codice EPREL è obbligatorio e deve essere un valore numerico";
  public static final String ERROR_CATEGORY_PRODUCTS = "Il campo Categoria è obbligatorio e deve essere coerente con la categoria selezionata";

  // Eprel Errors

  public static final class UploadKeyConstant {
    private UploadKeyConstant(){}
    public static final String EXTENSION_FILE_ERROR_KEY = "product.invalid.file.extension";
    public static final String MAX_ROW_FILE_ERROR_KEY = "product.invalid.file.maxrow";
    public static final String MAX_SIZE_FILE_ERROR_KEY = "product.invalid.file.maxsize";
    public static final String HEADER_FILE_ERROR_KEY = "product.invalid.file.header";
    public static final String REPORT_FORMAL_FILE_ERROR_KEY = "product.invalid.file.report";
    public static final String UNKNOWN_CATEGORY_ERROR_KEY = "product.invalid.file.category";
    public static final String EMPTY_FILE_ERROR_KEY = "product.invalid.file.empty";
    public static final String UPLOAD_ALREADY_IN_PROGRESS = "product.invalid.file.already_in_progress";
  }


  public static final class CsvValidationRules {
    private CsvValidationRules() {}
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


  //Eprel Messages Error
  public static final String ERROR_ORG = "Lo stato di verifica dell'organizzazione non è VERIFICATO";
  public static final String ERROR_TRADEMARK = "Lo stato di verifica del marchio non è VERIFICATO";
  public static final String ERROR_BLOCKED = "Il prodotto è BLOCCATO";
  public static final String ERROR_STATUS = "Lo stato non è PUBBLICATO";
  public static final String ERROR_PRODUCT_GROU = "La categoria EPREL non è compatibile con la categoria prevista";
  public static final String ERROR_ENERGY_CLASS = "La classe energetica non è conforme";
  public static final String  DUPLICATE_GTIN_EAN = "GTIN già presente in un'altra riga";
  public static final class EprelValidationRules {
    private EprelValidationRules() {}

    public static final EprelValidationRule ORG_VERIFICATION_STATUS_RULE =
        new EprelValidationRule((v, z) -> v != null && v.equalsIgnoreCase("VERIFIED"), ERROR_ORG);

    public static final EprelValidationRule TRADE_MARKER_VERIFICATION_STATUS_RULE =
      new EprelValidationRule((v, z) -> v != null &&  v.equalsIgnoreCase("VERIFIED"), ERROR_TRADEMARK);

    public static final EprelValidationRule BLOCKED_RULE =
      new EprelValidationRule((v, z) -> v != null && v.equalsIgnoreCase("FALSE"), ERROR_BLOCKED);

    public static final EprelValidationRule STATUS_RULE =
      new EprelValidationRule((v, z) -> v != null && v.equalsIgnoreCase("PUBLISHED"), ERROR_STATUS);

    public static final EprelValidationRule PRODUCT_GROUP_RULE =
      new EprelValidationRule((v, z) -> v != null && v.toLowerCase().startsWith(z.toLowerCase()), ERROR_PRODUCT_GROU);

    public static final EprelValidationRule ENERGY_CLASS_RULE =
      new EprelValidationRule((v, z) -> v != null && isEnergyClassValid(v,z), ERROR_ENERGY_CLASS);
  }

  public static final Pattern SUBJECT_PATTERN = Pattern.compile(".*/blobs/CSV/([^/]+)/([^/]+)/([^/]+\\.csv)$");

  public static final Map<String, String> ENERGY_CLASS_REQUIREMENTS = Map.of(
    WASHINGMACHINES, "A",
    WASHERDRIERS, "A",
    OVENS, "A",
    RANGEHOODS, "B",
    DISHWASHERS, "C",
    TUMBLEDRYERS, "C",
    REFRIGERATINGAPPL, "D"
  );

  public static final List<String> ENERGY_CLASS_ORDER = List.of(
    "APPP", "APP", "AP", "A", "B", "C", "D", "E", "F", "G"
  );

  public static final String STATUS_APPROVED = "APPROVED";

}
