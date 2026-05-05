package it.gov.pagopa.register.service.validator.rule.external;

import it.gov.pagopa.register.configuration.initiative.model.ValidationRule;
import it.gov.pagopa.register.service.validator.rule.RuleContext;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
import org.springframework.stereotype.Component;

@Component
public class EqualsExternalRuleExecutor implements RuleExecutor {

  @Override
  public String supports() {
    return "EQUALS_EXTERNAL";
  }

  @Override
  public boolean evaluate(
      ValidationRule rule,
      RuleContext context
  ) {
    ExternalRuleContext ctx = (ExternalRuleContext) context;

    Object externalValue = ctx.getExternalValue(rule.getField());
    if (externalValue == null) {
      return false;
    }

    return externalValue
        .toString()
        .equalsIgnoreCase(rule.getValue());
  }

  @Override
  public String errorMessage(
      ValidationRule rule,
      RuleContext context
  ) {
    return "Validazione esterna fallita sul campo '"
        + rule.getField()
        + "'";
  }
}
