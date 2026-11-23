package it.gov.pagopa.register.service.validator.rule.csv;

import it.gov.pagopa.register.model.initiative.ValidationRule;
import it.gov.pagopa.register.service.validator.rule.RuleContext;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
import org.springframework.stereotype.Component;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.ErrorKey.ERROR_CODE_PRODUCT;

@Component
public class RegexRuleExecutor implements RuleExecutor {
  public boolean evaluate(
    ValidationRule rule,
    RuleContext context
  ) {
    CsvRuleContext ctx = (CsvRuleContext) context;
    // CSV value
    String value = ctx.getValue(rule.getField());

    if (value == null || value.isBlank()) {
      // In the Bonus Elettrodomestici template the product code is optional.
      // The decoder template uses ERROR_CODE_PRODUCT_NOT_NULL and remains mandatory.
      if (ERROR_CODE_PRODUCT.equals(rule.getErrorKey())) {
        return true;
      }
      return false;
    }
    return value.matches(rule.getValue());
  }

  @Override
  public String supports() {
    return "REGEX";
  }
}
