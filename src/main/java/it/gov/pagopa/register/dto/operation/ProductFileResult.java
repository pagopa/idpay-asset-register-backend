package it.gov.pagopa.register.dto.operation;

public class ProductFileResult {

  private String status;     // "OK" o "KO"
  private String errorKey;   // presente solo se status == "KO"

  private ProductFileResult(String status, String errorKey) {
    this.status = status;
    this.errorKey = errorKey;
  }

  public static ProductFileResult ok() {
    return new ProductFileResult("OK", null);
  }

  public static ProductFileResult ko(String errorKey) {
    return new ProductFileResult("KO", errorKey);
  }

  public String getStatus() {
    return status;
  }

  public String getErrorKey() {
    return errorKey;
  }

}
