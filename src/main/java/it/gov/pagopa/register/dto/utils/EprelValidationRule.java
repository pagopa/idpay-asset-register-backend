package it.gov.pagopa.register.dto.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.function.BiPredicate;

@Data
@AllArgsConstructor
public class EprelValidationRule {

  private BiPredicate<String,String> rule;
  private String message;

  public boolean isValid(String value1, String value2) {
    return rule.test(value1, value2);
  }

}
