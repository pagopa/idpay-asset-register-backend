package it.gov.pagopa.register.configuration.initiative;

import lombok.Data;

@Data
public class CsvHeader {

  /**
   * Nome della colonna nel file CSV (es. "Marca")
   */
  private String name;

  /**
   * Nome logico del campo (es. "brand")
   */
  private String field;

  /**
   * Indica se il campo è obbligatorio
   */
  private boolean required;
}
