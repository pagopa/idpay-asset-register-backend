package it.gov.pagopa.register.service.validator.external.system.check;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class ExternalCheckResult {

  private final boolean valid;
  private final String errorMessage;
  private final Map<String, Object> externalData;

  public static ExternalCheckResult ok(Map<String, Object> externalData) {
    return new ExternalCheckResult(true, null,
      externalData != null ? externalData : Map.of());
  }

  public static ExternalCheckResult ko(String message) {
    return new ExternalCheckResult(false, message, Map.of());
  }
}
