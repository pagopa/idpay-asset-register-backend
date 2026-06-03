package it.gov.pagopa.register.dto.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdatedOperativeEmailResult {

  private final String status;
  private final String errorKey;

  private UpdatedOperativeEmailResult (String status, String errorKey) {
    this.status = status;
    this.errorKey = errorKey;
  }

  public static UpdatedOperativeEmailResult ok() {
    return new UpdatedOperativeEmailResult("OK", null);
  }

  public static UpdatedOperativeEmailResult ko(String errorKey) {
    return new UpdatedOperativeEmailResult("KO", errorKey);
  }
}
