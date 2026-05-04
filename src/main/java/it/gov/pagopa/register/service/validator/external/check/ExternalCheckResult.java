package it.gov.pagopa.register.service.validator.external.check;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExternalCheckResult {

  private final boolean valid;
  private final String errorMessage;

  public static ExternalCheckResult ok() {
    return new ExternalCheckResult(true, null);
  }

  public static ExternalCheckResult ko(String message) {
    return new ExternalCheckResult(false, message);
  }
}
