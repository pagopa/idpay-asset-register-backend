package it.gov.pagopa.register.dto.operation;

import org.apache.commons.csv.CSVRecord;

import java.util.List;
import java.util.Map;

public class ValidationResultDTO {
  private String status;     // "OK" or "KO"
  private String errorKey;   // i.e. EMPTY FILE, INVALID HEADER, ecc.
  private List<CSVRecord> invalidRecords;
  private Map<CSVRecord, String> errorMessages;


  public static ValidationResultDTO ok() {
    return new ValidationResultDTO("OK", null);
  }

  public static ValidationResultDTO ko(String errorKey) {
    return new ValidationResultDTO("KO", errorKey);
  }

  public static ValidationResultDTO ko(String errorKey, List<CSVRecord> invalidRecords, Map<CSVRecord, String> errorMessages) {
    return new ValidationResultDTO("KO", errorKey, invalidRecords, errorMessages);
  }

  // Base Constructor
  public ValidationResultDTO(String status, String errorKey) {
    this.status = status;
    this.errorKey = errorKey;
  }

  // Constructor with record error details
  public ValidationResultDTO(String status, String errorKey, List<CSVRecord> invalidRecords, Map<CSVRecord, String> errorMessages) {
    this.status = status;
    this.errorKey = errorKey;
    this.invalidRecords = invalidRecords;
    this.errorMessages = errorMessages;
  }

  public ValidationResultDTO(List<CSVRecord> invalidRecords, Map<CSVRecord, String> errorMessages) {
    this.invalidRecords = invalidRecords;
    this.errorMessages = errorMessages;
  }

  public ValidationResultDTO() {}

  public String getStatus() {
    return status;
  }

  public String getErrorKey() {
    return errorKey;
  }

  public List<CSVRecord> getInvalidRecords() {
    return invalidRecords;
  }

  public Map<CSVRecord, String> getErrorMessages() {
    return errorMessages;
  }


}
