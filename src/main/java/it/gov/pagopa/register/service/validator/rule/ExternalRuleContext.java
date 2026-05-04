package it.gov.pagopa.register.service.validator.rule;

import lombok.Data;

import java.util.Map;


@Data
public class ExternalRuleContext implements RuleContext {

  private final Map<String, Object> externalData;
  private final Map<String, Object> categoryParameters;
  private final String category;

}
