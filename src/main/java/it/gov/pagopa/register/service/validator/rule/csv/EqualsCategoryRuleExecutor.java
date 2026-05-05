package it.gov.pagopa.register.service.validator.rule.csv;

import it.gov.pagopa.register.configuration.initiative.model.ValidationRule;
import it.gov.pagopa.register.service.validator.rule.RuleContext;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
import org.springframework.stereotype.Component;

@Component
public class EqualsCategoryRuleExecutor implements RuleExecutor {

  @Override
  public String supports() {
    return "EQUALS_CATEGORY";
  }

  @Override
  public boolean evaluate(
      ValidationRule rule,
      RuleContext context
  ) {
    CsvRuleContext ctx = (CsvRuleContext) context;

    String csvCategory = ctx.getValue(rule.getField());
    String userCategory = ctx.getCategory();

    if (csvCategory == null) {
      return false;
    }

    return csvCategory.equalsIgnoreCase(userCategory);
  }

  @Override
  public String errorMessage(
      ValidationRule rule,
      RuleContext context
  ) {
    CsvRuleContext ctx = (CsvRuleContext) context;

    return "Errore di validazione: la categoria indicata nel CSV ('"
        + ctx.getValue(rule.getField())
        + "') non corrisponde alla categoria selezionata ('"
        + ctx.getCategory()
        + "')";
  }
}
