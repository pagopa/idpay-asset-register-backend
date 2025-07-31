package it.gov.pagopa.register.dto.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateResultDTO {
  private final String status;     // "OK" or "KO"
  private final String errorKey;   //

  public UpdateResultDTO(String status, String errorKey) {
    this.status = status;
    this.errorKey = errorKey;
  }

  public static UpdateResultDTO ok() {
    return new UpdateResultDTO("OK", null);
  }

  public static UpdateResultDTO ko(String errorKey) {
    return new UpdateResultDTO("KO", errorKey);
  }

}
