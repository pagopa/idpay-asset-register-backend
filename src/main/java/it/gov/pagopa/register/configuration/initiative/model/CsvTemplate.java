package it.gov.pagopa.register.configuration.initiative.model;

import lombok.Data;

import java.util.List;

@Data
public class CsvTemplate {

  /**
   * Header del CSV:
   * - nome reale nel file
   * - campo logico interno
   * - obbligatorietà
   * L’ordine della lista è l’ordine atteso nel CSV.
   */
  private List<CsvHeader> headers;

  /**
   * Regole di validazione
   */

  private List<ValidationRule> rules;
}
