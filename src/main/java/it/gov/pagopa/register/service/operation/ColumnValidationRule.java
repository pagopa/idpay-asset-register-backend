package it.gov.pagopa.register.service.operation;

import lombok.Data;

@Data
public class ColumnValidationRule {

  private String regex;
  private String message;

  public boolean isValid(String value) {
    return value != null && value.matches(regex);
  }
}
