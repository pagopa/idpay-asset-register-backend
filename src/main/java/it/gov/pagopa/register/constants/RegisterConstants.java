package it.gov.pagopa.register.constants;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterConstants {

  public static final Pattern SUBJECT_PATTERN = Pattern.compile(".*blobs/CSV/(.*?)-(.*?)-(.*?)-(.+\\.csv)$");

  public static final Map<String, String> ENERGY_CLASS_REQUIREMENTS = Map.of(
    "WASHINGMACHINES", "A",
    "WASHERDRIERS", "A",
    "OVENS", "A",
    "RANGEHOODS", "B",
    "DISHWASHERS", "C",
    "TUMBLEDRIERS", "C",
    "REFRIGERATINGAPPLIANCES", "D"
  );

  public static final List<String> ENERGY_CLASS_ORDER = Arrays.asList(
    "A+++", "A++", "A+", "A", "B", "C", "D", "E", "F", "G"
  );

}
