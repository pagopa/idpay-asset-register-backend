package it.gov.pagopa.register.dto.operation;

import lombok.Getter;
import org.apache.commons.csv.CSVRecord;

import java.util.List;
import java.util.Map;

@Getter
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


  public ValidationResultDTO(String status, String errorKey) {
    this.status = status;
    this.errorKey = errorKey;
  }

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


}
