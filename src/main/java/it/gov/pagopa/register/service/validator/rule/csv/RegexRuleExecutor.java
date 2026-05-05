package it.gov.pagopa.register.service.validator.rule.csv;

import it.gov.pagopa.register.configuration.initiative.model.ValidationRule;
import it.gov.pagopa.register.service.validator.rule.RuleContext;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
import org.springframework.stereotype.Component;

@Component
public class RegexRuleExecutor implements RuleExecutor {
  public boolean evaluate(
    ValidationRule rule,
    RuleContext context
  ) {
    CsvRuleContext ctx = (CsvRuleContext) context;
    String value = ctx.getValue(rule.getField());

    if (value == null) {
      return true;
    }

    return value.matches(rule.getValue());
  }

  @Override
  public String errorMessage(
    ValidationRule rule,
    RuleContext context
  ) {
    return "Errore di validazione sul campo '"
      + rule.getField()
      + "': formato non valido";
  }

  @Override
  public String supports() {
    return "REGEX";
  }
}
