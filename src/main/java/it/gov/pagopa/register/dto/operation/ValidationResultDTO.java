package it.gov.pagopa.register.dto.operation;

import lombok.Getter;
import org.apache.commons.csv.CSVRecord;

import java.util.List;
import java.util.Map;

@Getter
public class ValidationResultDTO {
  private final String status;     // "OK" or "KO"
  private final String errorKey;   // i.e. EMPTY FILE, INVALID HEADER, ecc.
  private List<CSVRecord> invalidRecords;
  private Map<CSVRecord, String> errorMessages;
  private List<CSVRecord> records;
  private List<String> headers;

  public static ValidationResultDTO ok(List<CSVRecord> records, List<String> headers) {
    return new ValidationResultDTO("OK", null,records,headers);
  }

  public static ValidationResultDTO ok() {
    return new ValidationResultDTO("OK", null);
  }

  public static ValidationResultDTO ko(String errorKey) {
    return new ValidationResultDTO("KO", errorKey);
  }


  public ValidationResultDTO(String status, String errorKey) {
    this.status = status;
    this.errorKey = errorKey;
  }
  public ValidationResultDTO(String status, String errorKey,List<CSVRecord> records, List<String> headers) {
    this.status = status;
    this.errorKey = errorKey;
    this.records = records;
    this.headers = headers;
  }
  public ValidationResultDTO(String status, String errorKey, List<CSVRecord> invalidRecords, Map<CSVRecord, String> errorMessages) {
    this.status = status;
    this.errorKey = errorKey;
    this.invalidRecords = invalidRecords;
    this.errorMessages = errorMessages;
  }


}
