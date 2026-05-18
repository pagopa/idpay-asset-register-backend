package it.gov.pagopa.register.service.validator.rule.external;

import it.gov.pagopa.register.model.initiative.ValidationRule;
import it.gov.pagopa.register.service.validator.rule.RuleContext;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
import org.springframework.stereotype.Component;

@Component
public class ProductGroupRuleExecutor implements RuleExecutor {

  @Override
  public String supports() {
    return "PRODUCT_GROUP_MATCH";
  }

  @Override
  public boolean evaluate(
      ValidationRule rule,
      RuleContext context
  ) {
    ExternalRuleContext ctx = (ExternalRuleContext) context;

    Object productGroupObj = ctx.getExternalData().get("productGroup");
    if (productGroupObj == null) {
      return false;
    }

    String productGroup = productGroupObj.toString().toLowerCase();
    String category = ctx.getCategory().toLowerCase();

    return productGroup.startsWith(category);
  }

}
