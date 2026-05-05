package it.gov.pagopa.register.configuration.initiative.model;

import lombok.Data;

@Data
public class CsvHeader {

  /**
   * Nome della colonna nel file CSV (es. "Marca")
   */
  private String name;

  /**
   * Indica se il campo è obbligatorio
   */
  private boolean required;
}
