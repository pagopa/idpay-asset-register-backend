package it.gov.pagopa.register.constants;

import java.util.Set;

public class AssetRegisterConstant {

  //private constructor to avoid instantiation
  private AssetRegisterConstant(){
  }

  public static final String CODICE_GTIN_EAN_REGEX = "^[a-zA-Z0-9]{1,14}$";
  public static final String MARCA_REGEX = "^.{1,100}$";
  public static final String MODELLO_REGEX = "^.{1,100}$";
  public static final String CODICE_PRODOTTO_REGEX = "^[a-zA-Z0-9 ]{0,100}$";
  public static final String PAESE_DI_PRODUZIONE_REGEX = "^.{2}$";
  public static final String CODICE_EPREL_REGEX = "^\\d+$";

  public static final String EPREL_ERROR = "EPREL_ERROR";
  public static final String FORMAL_ERROR = "FORMAL_ERROR";

  // CSV HEADER
  public static final String CODICE_EPREL = "Codice EPREL";
  public static final String CODICE_GTIN_EAN = "Codice GTIN/EAN";
  public static final String CODICE_PRODOTTO = "Codice Prodotto";
  public static final String CATEGORIA = "Categoria";
  public static final String PAESE_DI_PRODUZIONE = "Paese di Produzione";
  public static final String MARCA = "Marca";
  public static final String MODELLO = "Modello";

  public static final Set<String> CSV_HEADER_PRODOTTI = Set.of(
    CODICE_EPREL,
    CODICE_GTIN_EAN,
    CODICE_PRODOTTO,
    CATEGORIA,
    PAESE_DI_PRODUZIONE
  );

  public static final Set<String> CSV_HEADER_PIANI_COTTURA = Set.of(
    CODICE_GTIN_EAN,
    CODICE_PRODOTTO,
    CATEGORIA,
    PAESE_DI_PRODUZIONE,
    MARCA,
    MODELLO
  );

  public static final String WASHINGMACHINES = "WASHINGMACHINES";
  public static final String WASHERDRIERS = "WASHERDRIERS";
  public static final String OVENS = "OVENS";
  public static final String RANGEHOODS = "RANGEHOODS";
  public static final String DISHWASHERS = "DISHWASHERS";
  public static final String TUMBLEDRIERS = "TUMBLEDRIERS";
  public static final String REFRIGERATINGAPPL = "REFRIGERATINGAPPL";
  public static final String COOKINGHOBS = "COOKINGHOBS";

  public static final Set<String> CATEGORIE_PRODOTTI = Set.of(
    WASHINGMACHINES,
    WASHERDRIERS,
    OVENS,
    RANGEHOODS,
    DISHWASHERS,
    TUMBLEDRIERS,
    REFRIGERATINGAPPL
  );

  // Errors

  public static final String ERROR_GTIN_EAN = "Il Codice GTIN/EAN è obbligatorio e deve essere univoco ed alfanumerico e lungo al massimo 14 caratteri";
  public static final String ERROR_CATEGORIA_COOKINGHOBS = "Il campo Categoria è obbligatorio e deve contenere il valore fisso 'COOKINGHOBS'";
  public static final String ERROR_MARCA = "Il campo Marca è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri";
  public static final String ERROR_MODELLO = "Il campo Modello è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri";
  public static final String ERROR_CODICE_PRODOTTO = "Il Codice prodotto non deve contenere caratteri speciali o lettere accentate e deve essere lungo al massimo 100 caratteri";
  public static final String ERROR_PAESE_DI_PRODUZIONE = "Il Paese di Produzione è obbligatorio e deve essere composto da esattamente 2 caratteri";
  public static final String ERROR_CODICE_EPREL = "Il Codice EPREL è obbligatorio e deve essere un valore numerico";
  public static final String ERROR_CATEGORIA_PRODOTTI = "Il campo Categoria è obbligatorio e deve contenere il valore fisso ";
}
