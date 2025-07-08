package it.gov.pagopa.register.constants;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterConstants {

  private RegisterConstants(){
  }

  public static final Pattern SUBJECT_PATTERN = Pattern.compile(".*/blobs/CSV/([^/]+)/([^/]+)/([^/]+\\.csv)$");

  public static final Map<String, String> ENERGY_CLASS_REQUIREMENTS = Map.of(
    "WASHINGMACHINES", "A",
    "WASHERDRIERS", "A",
    "OVENS", "A",
    "RANGEHOODS", "B",
    "DISHWASHERS", "C",
    "TUMBLEDRYERS", "C",
    "REFRIGERATINGAPPL", "D"
  );

  public static final List<String> ENERGY_CLASS_ORDER = Arrays.asList(
    "A+++", "A++", "A+", "A", "B", "C", "D", "E", "F", "G"
  );

  public static final class CsvRecord {
    public static final String PRODUCT_CODE = "Codice Prodotto";
    public static final String EPREL_CODE = "Codice EPREL";
    public static final String PRODUCTION_COUNTRY = "Paese di Produzione";
    public static final String GTIN_EAN_CODE = "Codice GTIN/EAN";
    public static final String BRAND = "Marca";
    public static final String MODEL = "Modello";
    public static final String CATEGORY_COOKINGHOBS = "COOKINGHOBS";
    public static final String STATUS_APPROVED = "APPROVED";

    private CsvRecord() {}
  }

}
