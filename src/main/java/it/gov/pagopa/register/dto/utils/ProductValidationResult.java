package it.gov.pagopa.register.dto.utils;

import it.gov.pagopa.register.model.operation.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.csv.CSVRecord;

import java.util.List;
import java.util.Map;


@AllArgsConstructor
@Getter
@Setter
public class ProductValidationResult {
  private final Map<String, Product> validRecords;
  private final List<CSVRecord> invalidRecords;
  private final Map<CSVRecord, String> errorMessages;

}
