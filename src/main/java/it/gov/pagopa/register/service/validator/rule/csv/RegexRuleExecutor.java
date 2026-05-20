package it.gov.pagopa.register.service.validator.rule.csv;

import it.gov.pagopa.register.model.initiative.ValidationRule;
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
    // CSV value
    String value = ctx.getValue(rule.getField());

    if (value == null) {
      return false;
    }
    return value.matches(rule.getValue());
  }

  @Override
  public String supports() {
    return "REGEX";
  }
}
