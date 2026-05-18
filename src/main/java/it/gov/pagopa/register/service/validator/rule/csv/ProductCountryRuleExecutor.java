package it.gov.pagopa.register.service.validator.rule.csv;

import it.gov.pagopa.register.model.initiative.ValidationRule;
import it.gov.pagopa.register.service.validator.rule.RuleContext;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class ProductCountryRuleExecutor implements RuleExecutor {
  private static final Set<String> ISO_COUNTRIES =Set.of(Locale.getISOCountries());

  @Override
  public String supports() {
    return "PRODUCT_COUNTRY";
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

    return ISO_COUNTRIES.contains(value);
  }

}


