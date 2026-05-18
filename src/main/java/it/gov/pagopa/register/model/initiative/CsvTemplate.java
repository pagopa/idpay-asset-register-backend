package it.gov.pagopa.register.model.initiative;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CsvTemplate {

  /**
   * Header del CSV:
   * - nome reale nel file
   * - campo logico interno
   * - obbligatorietà
   * L’ordine della lista è l’ordine atteso nel CSV.
   */
  private List<String> headers;

  /**
   * Regole di validazione
   */

  private List<ValidationRule> rules;
}
