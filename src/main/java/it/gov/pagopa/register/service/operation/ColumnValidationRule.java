package it.gov.pagopa.register.service.operation;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.function.BiPredicate;

@Data
@AllArgsConstructor
public class ColumnValidationRule {

  private BiPredicate<String,String> rule;
  private String message;

  public boolean isValid(String value1, String value2) {
    return rule.test(value1, value2);
  }

}
