package it.gov.pagopa.register.dto.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductFileResult {

  private final String status;
  private final String errorKey;
  private String productFileId;

  private ProductFileResult(String status, String errorKey) {
    this.status = status;
    this.errorKey = errorKey;
  }

  private ProductFileResult(String status, String errorKey, String productFileId) {
    this.status = status;
    this.errorKey = errorKey;
    this.productFileId = productFileId;
  }

  public static ProductFileResult ok() {
    return new ProductFileResult("OK", null);
  }

  public static ProductFileResult ko(String errorKey) {
    return new ProductFileResult("KO", errorKey);
  }

  public static ProductFileResult ko(String errorKey, String productFileId) {
    return new ProductFileResult("KO", errorKey, productFileId);
  }

}
