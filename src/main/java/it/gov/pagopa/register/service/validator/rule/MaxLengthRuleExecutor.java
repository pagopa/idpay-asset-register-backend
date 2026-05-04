package it.gov.pagopa.register.service.validator.rule;

import it.gov.pagopa.register.configuration.initiative.ValidationRule;
import org.springframework.stereotype.Component;

@Component
public class MaxLengthRuleExecutor implements RuleExecutor {

  @Override
  public String supports() {
    return "MAX_LENGTH";
  }

  @Override
  public boolean evaluate(
      ValidationRule rule,
      RuleContext ctx
  ) {
    CsvRuleContext context = (CsvRuleContext) ctx;
    String value = context.getValue(rule.getField());

    return value == null || Integer.parseInt(value) <= Integer.parseInt(rule.getValue());
  }

  @Override
  public String errorMessage(
      ValidationRule rule,
      RuleContext ctx
  ) {
    return "Errore di validazione '"
        + rule.getField()
        + "': la lunghezza supera il limite massimo consentito ("
        + rule.getValue()
        + " caratteri)";
  }
}
