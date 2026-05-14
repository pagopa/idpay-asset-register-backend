package it.gov.pagopa.register.service.validator.rule.csv;

import it.gov.pagopa.register.configuration.initiative.model.ValidationRule;
import it.gov.pagopa.register.service.validator.rule.RuleContext;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
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
      RuleContext context
  ) {

    CsvRuleContext ctx = (CsvRuleContext) context;
    String value = ctx.getValue(rule.getField());

    if (value == null) {
      return false;
    }

    int maxLength = Integer.parseInt(rule.getValue());
    return value.length() <= maxLength;
  }


}
