package it.gov.pagopa.register.dto.operation;

import lombok.Getter;

@Getter
public class ProductFileResult {

  private String status;     // "OK" o "KO"
  private String errorKey;   // presente solo se status == "KO"
  private String productFileId; // id of ko file

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
