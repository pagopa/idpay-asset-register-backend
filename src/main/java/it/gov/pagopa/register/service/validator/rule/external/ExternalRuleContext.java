package it.gov.pagopa.register.service.validator.rule.external;

import it.gov.pagopa.register.service.validator.rule.RuleContext;
import lombok.Data;

import java.util.Map;


@Data
public class ExternalRuleContext implements RuleContext {

  private final Map<String, Object> externalData;
  private final Map<String, Object> categoryParameters;
  private final String category;

  public Object getExternalValue(String field) {
    return externalData.get(field);
  }

  public String getCategoryParameter(String key) {
    Object value = categoryParameters.get(key);
    return value != null ? value.toString() : null;
  }

}
