package it.gov.pagopa.register.service.operation;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.function.Predicate;

@Data
@AllArgsConstructor
public class ColumnValidationRule {

  private Predicate<String> rule;
  private String message;

  public boolean isValid(String value) {
    return value != null && rule.test(value);
  }
}
