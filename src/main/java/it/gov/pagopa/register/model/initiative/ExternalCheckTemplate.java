package it.gov.pagopa.register.model.initiative;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ExternalCheckTemplate {

  /**
   * Identificativo tecnico del check (EPREL, ALTRO_ENTE, ...)
   */
  private String type;

  private String inputField;

  /**
   * Campi da recuperare dalla risposta esterna
   */
  private List<String> fieldsToRetrieve;

  /**
   * Regole di validazione
   */
  private List<ValidationRule> rules;
}
