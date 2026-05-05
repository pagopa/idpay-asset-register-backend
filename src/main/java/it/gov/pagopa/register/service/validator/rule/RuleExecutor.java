package it.gov.pagopa.register.service.validator.rule;

import it.gov.pagopa.register.configuration.initiative.model.ValidationRule;

public interface RuleExecutor {

  /**
   * Tipo di regola supportata
   */
  String supports();

  /**
   * Valida una singola regola
   */

  boolean evaluate(
    ValidationRule rule,
    RuleContext context
  );

  String errorMessage(
    ValidationRule rule,
    RuleContext context
  );

}
