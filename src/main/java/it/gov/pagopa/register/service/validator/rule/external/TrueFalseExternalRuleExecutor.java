package it.gov.pagopa.register.service.validator.rule.external;

import it.gov.pagopa.register.configuration.initiative.model.ValidationRule;
import it.gov.pagopa.register.service.validator.rule.RuleContext;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
import org.springframework.stereotype.Component;

@Component
public class TrueFalseExternalRuleExecutor implements RuleExecutor {

  @Override
  public String supports() {
    return "TRUE_FALSE_EXTERNAL";
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

    String ruleValue = rule.getValue();

    if ("true".equalsIgnoreCase(ruleValue) || "false".equalsIgnoreCase(ruleValue)) {
      Boolean extBool = Boolean.valueOf(externalValue.toString());
      Boolean ruleBool = Boolean.valueOf(ruleValue);
      return extBool.equals(ruleBool);
    }

    return externalValue
      .toString()
      .equalsIgnoreCase(ruleValue);
  }

}
