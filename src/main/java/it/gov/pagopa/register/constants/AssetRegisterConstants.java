package it.gov.pagopa.register.constants;

import it.gov.pagopa.register.enums.UploadCsvStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class AssetRegisterConstants {

  private AssetRegisterConstants() {}

  // =========================================================
  // FILE / REPORT
  // =========================================================
  public static final String CSV = ".csv";
  public static final String REPORT_PARTIAL_ERROR = "Report/Partial/";
  public static final String REPORT_FORMAL_ERROR = "Report/Formal/";

  public static final Pattern SUBJECT_PATTERN =
    Pattern.compile(".*/blobs/CSV/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/]+\\.csv)$");

  // =========================================================
  // UPLOAD
  // =========================================================
  public static final List<String> BLOCKING_STATUSES = List.of(
    UploadCsvStatus.IN_PROCESS.name(),
    UploadCsvStatus.UPLOADED.name()
  );

  public static final class UploadError {
    private UploadError() {}

    public static final String EXTENSION_FILE_ERROR_KEY = "product.invalid.file.extension";
    public static final String MAX_ROW_FILE_ERROR_KEY = "product.invalid.file.maxrow";
    public static final String MAX_SIZE_FILE_ERROR_KEY = "product.invalid.file.maxsize";
    public static final String HEADER_FILE_ERROR_KEY = "product.invalid.file.header";
    public static final String REPORT_FORMAL_FILE_ERROR_KEY = "product.invalid.file.report";
    public static final String UNKNOWN_CATEGORY_ERROR_KEY = "product.invalid.file.category";
    public static final String EMPTY_FILE_ERROR_KEY = "product.invalid.file.empty";
    public static final String INITIATIVE_CONFIG_ERROR = "product.invalid.file.initiative_config";
    public static final String UPLOAD_ALREADY_IN_PROGRESS = "product.invalid.file.already_in_progress";
    public static final String NOT_ENABLED_ERRORE_KEY = "product.invalid.file.permission";
  }

  public static final class UploadEmailError {
    private UploadEmailError() {}

    public static final String EMAIL_WRONG_ERROR_KEY = "update.email.wrong.format";
    public static final String EMAIL_INITATIVE_ERROR_KEY = "update.email.invalid.initiative";
  }

  // =========================================================
  // CSV HEADER
  // =========================================================
  public static final class CsvHeader {
    private CsvHeader() {}

    public static final String CODE_EPREL = "Codice EPREL";
    public static final String CODE_GTIN_EAN = "Codice GTIN/EAN";
    public static final String CODE_PRODUCT = "Codice Prodotto";
    public static final String CATEGORY = "Categoria";
    public static final String COUNTRY_OF_PRODUCTION = "Paese di Produzione";
    public static final String BRAND = "Marca";
    public static final String MODEL = "Modello";
  }

  // =========================================================
  // CATEGORIES
  // =========================================================
  public static final class Category {

    private Category() {}

    // --- BE --- //

    public static final String WASHINGMACHINES = "WASHINGMACHINES";
    public static final String WASHERDRIERS = "WASHERDRIERS";
    public static final String OVENS = "OVENS";
    public static final String RANGEHOODS = "RANGEHOODS";
    public static final String DISHWASHERS = "DISHWASHERS";
    public static final String TUMBLEDRYERS = "TUMBLEDRYERS";
    public static final String REFRIGERATINGAPPL = "REFRIGERATINGAPPL";
    public static final String COOKINGHOBS = "COOKINGHOBS";
    public static final String WASHINGMACHINES_IT_P = "Lavatrici";
    public static final String WASHERDRIERS_IT_P = "Lavasciuga";
    public static final String OVENS_IT_P = "Forni";
    public static final String RANGEHOODS_IT_P = "Cappe_Da_Cucina";
    public static final String DISHWASHERS_IT_P = "Lavastoviglie";
    public static final String TUMBLEDRYERS_IT_P = "Asciugatrici";
    public static final String REFRIGERATINGAPPL_IT_P = "Apparecchi_di_refrigerazione";
    public static final String COOKINGHOBS_IT_P = "Piani_Cottura";
    public static final String WASHINGMACHINES_IT_S = "Lavatrice";
    public static final String WASHERDRIERS_IT_S = "Lavasciuga";
    public static final String OVENS_IT_S = "Forno";
    public static final String RANGEHOODS_IT_S = "Cappa da cucina";
    public static final String DISHWASHERS_IT_S = "Lavastoviglie";
    public static final String TUMBLEDRYERS_IT_S = "Asciugatrice";
    public static final String REFRIGERATINGAPPL_IT_S = "Apparecchio di refrigerazione";
    public static final String COOKINGHOBS_IT_S = "Piano cottura";

    // --- BD --- //

    public static final String SATELLITE_CODE = "DS";
    public static final String TERRESTRIAL_CODE = "DT";
    public static final String TERRESTRIAL_VIA_CABLE_CODE = "DTC";
    public static final String TERRESTRIAL_AND_SATELLITE_CODE = "DTS";
    public static final String TERRESTRIAL_SATELLITE_AND_VIA_CABLE_CODE = "DTSC";
    public static final String SATELLITE =  "SATELLITE";
    public static final String TERRESTRIAL = "TERRESTRIAL";
    public static final String TERRESTRIAL_VIA_CABLE = "TERRESTRIAL_VIA_CABLE";
    public static final String TERRESTRIAL_AND_SATELLITE = "TERRESTRIAL_AND_SATELLITE";
    public static final String TERRESTRIAL_SATELLITE_AND_VIA_CABLE = "TERRESTRIAL_SATELLITE_AND_VIA_CABLE";
    public static final String SATELLITE_IT_S =  "Satellitare";
    public static final String TERRESTRIAL_IT_S = "Terrestre";
    public static final String TERRESTRIAL_VIA_CABLE_IT_S = "Terrestre via Cavo";
    public static final String TERRESTRIAL_AND_SATELLITE_IT_S = "Terrestre e Satellitare";
    public static final String TERRESTRIAL_SATELLITE_AND_VIA_CABLE_IT_S = "Terrestre, Satellitare e via Cavo";
    public static final String SATELLITE_IT_P =  "Satellitare";
    public static final String TERRESTRIAL_IT_P = "Terrestre";
    public static final String TERRESTRIAL_VIA_CABLE_IT_P = "Terrestre_via_Cavo";
    public static final String TERRESTRIAL_AND_SATELLITE_IT_P = "Terrestre_e_Satellitare";
    public static final String TERRESTRIAL_SATELLITE_AND_VIA_CABLE_IT_P = "Terrestre_Satellitare_e_via_Cavo";

    // -------- LABELS --------
    public static final class Labels {
      private Labels() {}

      public static final Map<String, String> CATEGORIES_FOR_PRODUCT_NAME = Map.ofEntries(
        /* BE */
        Map.entry(WASHINGMACHINES, WASHINGMACHINES_IT_S),
        Map.entry(WASHERDRIERS, WASHERDRIERS_IT_S),
        Map.entry(OVENS, OVENS_IT_S),
        Map.entry(RANGEHOODS, RANGEHOODS_IT_S),
        Map.entry(DISHWASHERS, DISHWASHERS_IT_S),
        Map.entry(TUMBLEDRYERS, TUMBLEDRYERS_IT_S),
        Map.entry(REFRIGERATINGAPPL, REFRIGERATINGAPPL_IT_S),
        Map.entry(COOKINGHOBS, COOKINGHOBS_IT_S),

        /* BD */
        Map.entry(SATELLITE_CODE, SATELLITE_IT_S),
        Map.entry(TERRESTRIAL_CODE, TERRESTRIAL_IT_S),
        Map.entry(TERRESTRIAL_VIA_CABLE_CODE, TERRESTRIAL_VIA_CABLE_IT_S),
        Map.entry(TERRESTRIAL_AND_SATELLITE_CODE, TERRESTRIAL_AND_SATELLITE_IT_S),
        Map.entry(TERRESTRIAL_SATELLITE_AND_VIA_CABLE_CODE, TERRESTRIAL_SATELLITE_AND_VIA_CABLE_IT_S)
      );

      public static final Map<String, String> CATEGORIES_FOR_DTO = Map.ofEntries(
        /* BE */
        Map.entry(WASHINGMACHINES, WASHINGMACHINES_IT_S),
        Map.entry(WASHERDRIERS, WASHERDRIERS_IT_S),
        Map.entry(OVENS, OVENS_IT_S),
        Map.entry(RANGEHOODS, RANGEHOODS_IT_S),
        Map.entry(DISHWASHERS, DISHWASHERS_IT_S),
        Map.entry(TUMBLEDRYERS, TUMBLEDRYERS_IT_S),
        Map.entry(REFRIGERATINGAPPL, REFRIGERATINGAPPL_IT_S),
        Map.entry(COOKINGHOBS, COOKINGHOBS_IT_S),

        /* BD */
        Map.entry(SATELLITE_CODE, SATELLITE_IT_S),
        Map.entry(TERRESTRIAL_CODE, TERRESTRIAL_IT_S),
        Map.entry(TERRESTRIAL_VIA_CABLE_CODE, TERRESTRIAL_VIA_CABLE_IT_S),
        Map.entry(TERRESTRIAL_AND_SATELLITE_CODE, TERRESTRIAL_AND_SATELLITE_IT_S),
        Map.entry(TERRESTRIAL_SATELLITE_AND_VIA_CABLE_CODE, TERRESTRIAL_SATELLITE_AND_VIA_CABLE_IT_S)
      );

      public static final Map<String, String> CATEGORIES_FOR_CSV_CHECK = Map.ofEntries(
        /* BE */
        Map.entry(WASHINGMACHINES, WASHINGMACHINES_IT_S),
        Map.entry(WASHERDRIERS, WASHERDRIERS_IT_S),
        Map.entry(OVENS, OVENS_IT_S),
        Map.entry(RANGEHOODS, RANGEHOODS_IT_S),
        Map.entry(DISHWASHERS, DISHWASHERS_IT_S),
        Map.entry(TUMBLEDRYERS, TUMBLEDRYERS_IT_S),
        Map.entry(REFRIGERATINGAPPL, REFRIGERATINGAPPL_IT_S),
        Map.entry(COOKINGHOBS, COOKINGHOBS_IT_S),

        /* BD */
        Map.entry(SATELLITE_CODE, SATELLITE_CODE),
        Map.entry(TERRESTRIAL_CODE, TERRESTRIAL_CODE),
        Map.entry(TERRESTRIAL_VIA_CABLE_CODE, TERRESTRIAL_VIA_CABLE_CODE),
        Map.entry(TERRESTRIAL_AND_SATELLITE_CODE, TERRESTRIAL_AND_SATELLITE_CODE),
        Map.entry(TERRESTRIAL_SATELLITE_AND_VIA_CABLE_CODE, TERRESTRIAL_SATELLITE_AND_VIA_CABLE_CODE)
      );

      public static final Map<String, String> CATEGORIES_FOR_FILENAME = Map.ofEntries(
        /* BE */
        Map.entry(WASHINGMACHINES, WASHINGMACHINES_IT_P),
        Map.entry(WASHERDRIERS, WASHERDRIERS_IT_P),
        Map.entry(OVENS, OVENS_IT_P),
        Map.entry(RANGEHOODS, RANGEHOODS_IT_P),
        Map.entry(DISHWASHERS, DISHWASHERS_IT_P),
        Map.entry(TUMBLEDRYERS, TUMBLEDRYERS_IT_P),
        Map.entry(REFRIGERATINGAPPL, REFRIGERATINGAPPL_IT_P),
        Map.entry(COOKINGHOBS, COOKINGHOBS_IT_P),
        /* BD */
        Map.entry(SATELLITE_CODE, SATELLITE_IT_P),
        Map.entry(TERRESTRIAL_CODE, TERRESTRIAL_IT_P),
        Map.entry(TERRESTRIAL_VIA_CABLE_CODE, TERRESTRIAL_VIA_CABLE_IT_P),
        Map.entry(TERRESTRIAL_AND_SATELLITE_CODE, TERRESTRIAL_AND_SATELLITE_IT_P),
        Map.entry(TERRESTRIAL_SATELLITE_AND_VIA_CABLE_CODE, TERRESTRIAL_SATELLITE_AND_VIA_CABLE_IT_P)
      );
    }
  }

  // =========================================================
  // ERRORS
  // =========================================================
  public static final class ErrorKey {
    private ErrorKey() {}

    // EPREL Errors
    public static final String ERROR_ORG = "ERROR_ORG";
    public static final String ERROR_TRADEMARK = "ERROR_TRADEMARK";
    public static final String ERROR_BLOCKED = "ERROR_BLOCKED";
    public static final String ERROR_STATUS = "ERROR_STATUS";
    public static final String ERROR_PRODUCT_GROUP = "ERROR_PRODUCT_GROUP";
    public static final String ERROR_ENERGY_CLASS = "ERROR_ENERGY_CLASS";
    public static final String DUPLICATE_GTIN_EAN = "DUPLICATE_GTIN_EAN";
    public static final String DIFFERENT_ORGANIZATIONID = "DIFFERENT_ORGANIZATIONID";
    public static final String STATUS_NOT_VALID = "STATUS_NOT_VALID";

    // CSV Errors
    public static final String ERROR_GTIN_EAN = "ERROR_GTIN_EAN";
    public static final String ERROR_BRAND = "ERROR_BRAND";
    public static final String ERROR_MODEL = "ERROR_MODEL";
    public static final String ERROR_CODE_PRODUCT = "ERROR_CODE_PRODUCT";
    public static final String ERROR_CODE_PRODUCT_NOT_NULL = "ERROR_CODE_PRODUCT_NOT_NULL";
    public static final String ERROR_COUNTRY_OF_PRODUCTION = "ERROR_COUNTRY_OF_PRODUCTION";
    public static final String ERROR_CATEGORY_PRODUCTS = "ERROR_CATEGORY_PRODUCTS";
    public static final String ERROR_CODE_EPREL = "ERROR_CODE_EPREL";
  }

  public static final Map<String, String> ERROR_MAP = Map.ofEntries(

    // Eprel Error
    Map.entry(ErrorKey.ERROR_ORG, "Il produttore non risulta nell'elenco della Banca dati europea dei prodotti per l'etichettatura energetica; è necessario completare le informazioni sul portale EPREL"),
    Map.entry(ErrorKey.ERROR_TRADEMARK, "Il marchio associato al prodotto non risulta nell'elenco della Banca dati europea dei prodotti per l'etichettatura energetica - EPREL"),
    Map.entry(ErrorKey.ERROR_BLOCKED, "Il prodotto risulta bloccato nell'elenco della Banca dati europea dei prodotti per l'etichettatura energetica - EPREL"),
    Map.entry(ErrorKey.ERROR_STATUS, "Il prodotto non è presente o caricato nell'elenco della Banca dati europea dei prodotti per l'etichettatura energetica - EPREL"),
    Map.entry(ErrorKey.ERROR_PRODUCT_GROUP, "La categoria presente sulla Banca dati europea dei prodotti per l'etichettatura energetica - EPREL non è coerente con quella del file CSV"),
    Map.entry(ErrorKey.ERROR_ENERGY_CLASS, "La classe energetica non è conforme con quella prevista nel DM del {}"),
    Map.entry(ErrorKey.DUPLICATE_GTIN_EAN, "Il codice GTIN indicato nel file CSV è un duplicato"),
    Map.entry(ErrorKey.DIFFERENT_ORGANIZATIONID, "Il prodotto indicato è associato ad un altro produttore"),
    Map.entry(ErrorKey.STATUS_NOT_VALID, "Il prodotto è sottoposto alle verifiche previste dal DM del {} e pertanto non è possibile variare le informazioni ad esso collegate"),

    // Csv Errors
    Map.entry(ErrorKey.ERROR_GTIN_EAN, "Il Codice GTIN/EAN è obbligatorio e deve essere univoco ed alfanumerico e lungo al massimo 14 caratteri"),
    Map.entry(ErrorKey.ERROR_BRAND, "Il campo Marca è obbligatorio, non deve contenere caratteri speciali o lettere accentate e deve essere lungo al massimo 100 caratteri"),
    Map.entry(ErrorKey.ERROR_MODEL, "Il campo Modello è obbligatorio, non deve contenere caratteri speciali o lettere accentate e deve essere lungo al massimo 100 caratteri"),
    Map.entry(ErrorKey.ERROR_CODE_PRODUCT, "Il Codice prodotto non deve contenere caratteri speciali o lettere accentate e deve essere lungo al massimo 100 caratteri"),
    Map.entry(ErrorKey.ERROR_CODE_PRODUCT_NOT_NULL, "Il campo Codice Prodotto è obbligatorio, non deve contenere caratteri speciali o lettere accentate e deve essere lungo al massimo 100 caratteri"),
    Map.entry(ErrorKey.ERROR_COUNTRY_OF_PRODUCTION, "Paese di Produzione non è un ISO 3166 valido o non è in maiuscolo"),
    Map.entry(ErrorKey.ERROR_CATEGORY_PRODUCTS, "Il campo Categoria è obbligatorio e deve essere \"{}\""),
    Map.entry(ErrorKey.ERROR_CODE_EPREL, "Il Codice EPREL è obbligatorio e deve essere un valore numerico")
  );

  // =========================================================
  // REFRIGERATOR
  // =========================================================
  public static final class Refrigerator {
    private Refrigerator() {}

    public static final String PANTRY = "PANTRY";
    public static final String WINE_STORAGE = "WINE_STORAGE";
    public static final String CELLAR = "CELLAR";
    public static final String FRESH_FOOD = "FRESH_FOOD";
    public static final String CHILL = "CHILL";
    public static final String VARIABLE_TEMP = "VARIABLE_TEMP";

    public static final Set<String> REFRIGERATORS_CATEGORY = Set.of(
      PANTRY, WINE_STORAGE, CELLAR, FRESH_FOOD, CHILL
    );

    public static final String FREEZER_IT = "Congelatore";
    public static final String REFRIGERATOR_IT = "Frigorifero";
  }

  // =========================================================
  // UPDATE ERRORS
  // =========================================================
  public static final class UpdateError {
    private UpdateError() {}

    public static final String MIXED_STATUS_ERROR_KEY = "product.invalid.update.mixedStatus";
    public static final String INVALID_CURRENT_STATUS_ERROR_KEY = "product.invalid.update.currentStatus";
    public static final String PRODUCT_NOT_FOUND_ERROR_KEY = "product.invalid.update.notFound";
    public static final String TRANSITION_NOT_ALLOWED_ERROR_KEY = "product.invalid.update.transitionNotAllowed";
    public static final String INITIATIVE_NOT_FOUND_ERROR_KEY = "product.invalid.update.initiativeNotFound";
  }

  // TEST
  public static final String USERNAME = "USERNAME";

  //
  public static final class EprelField {
    private EprelField() {}

    public static final String STATUS = "status";
    public static final String ENERGY_CLASS = "energyClass";
    public static final String ENERGY_CLASS_WASH = "energyClassWash";
    public static final String BLOCKED = "blocked";
    public static final String PRODUCT_GROUP = "productGroup";
    public static final String ORG_VERIFICATION_STATUS = "orgVerificationStatus";
    public static final String TRADEMARK_VERIFICATION_STATUS = "trademarkVerificationStatus";
    public static final String SUPPLIER_OR_TRADEMARK = "supplierOrTrademark";
    public static final String MODEL_IDENTIFIER = "modelIdentifier";
    public static final String RATED_CAPACITY = "ratedCapacity";
    public static final String RATED_CAPACITY_WASH = "ratedCapacityWash"; // attenzione al naming
    public static final String CAVITIES = "cavities";
    public static final String TOTAL_VOLUME = "totalVolume";
  }
}
