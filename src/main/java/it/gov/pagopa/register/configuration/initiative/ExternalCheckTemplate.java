package it.gov.pagopa.register.configuration.initiative;

import lombok.Data;

import java.util.List;

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
