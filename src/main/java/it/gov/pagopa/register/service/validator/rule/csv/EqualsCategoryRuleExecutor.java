package it.gov.pagopa.register.service.validator.rule.csv;

import it.gov.pagopa.register.model.initiative.ValidationRule;
import it.gov.pagopa.register.service.validator.rule.RuleContext;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
import org.springframework.stereotype.Component;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.CATEGORIES_TO_IT_S;

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
    String userCategory = CATEGORIES_TO_IT_S.get(ctx.getCategory());

    if (csvCategory == null) {
      return false;
    }

    return csvCategory.equals(userCategory);
  }

}
