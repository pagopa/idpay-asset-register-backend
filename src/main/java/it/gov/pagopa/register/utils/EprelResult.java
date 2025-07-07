package it.gov.pagopa.register.utils;

import it.gov.pagopa.register.model.operation.Product;
import org.apache.commons.csv.CSVRecord;

import java.util.List;
import java.util.Map;


public record EprelResult(List<Product> validRecords,
                          List<CSVRecord> invalidRecords,
                          Map<CSVRecord, String> errorMessages) {

}
