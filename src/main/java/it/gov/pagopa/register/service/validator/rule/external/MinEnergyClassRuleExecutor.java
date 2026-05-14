package it.gov.pagopa.register.service.validator.rule.external;

import it.gov.pagopa.register.configuration.initiative.model.ValidationRule;
import it.gov.pagopa.register.service.validator.rule.RuleContext;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MinEnergyClassRuleExecutor implements RuleExecutor {

  @Override
  public String supports() {
    return "MIN_ENERGY_CLASS";
  }

  @Override
  public boolean evaluate(
      ValidationRule rule,
      RuleContext context
  ) {
    ExternalRuleContext ctx = (ExternalRuleContext) context;

    Object actualObj = ctx.getExternalData().get("energyClass");
    if (actualObj == null) {
      return false;
    }

    String actualClass = actualObj.toString();
    String minRequired =
        ctx.getCategoryParameters().get(rule.getParam()).toString();

    List<String> order = rule.getOrder();

    int actualIndex = order.indexOf(actualClass);
    int requiredIndex = order.indexOf(minRequired);

    if (actualIndex < 0 || requiredIndex < 0) {
      return false;
    }

    return actualIndex <= requiredIndex;
  }

}
