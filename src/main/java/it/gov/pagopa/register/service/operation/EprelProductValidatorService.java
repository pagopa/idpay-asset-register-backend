package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.config.EprelValidationConfig;
import it.gov.pagopa.register.connector.eprel.EprelConnector;
import it.gov.pagopa.register.utils.EprelProduct;
import it.gov.pagopa.register.utils.EprelResult;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.utils.EprelValidationRule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.util.*;

import static it.gov.pagopa.register.constants.RegisterConstants.CsvRecord.EPREL_CODE;
import static it.gov.pagopa.register.model.operation.mapper.ProductMapper.mapEprelToProduct;

@Service
@RequiredArgsConstructor
public class EprelProductValidatorService {


  private final EprelValidationConfig eprelValidationConfig;
  private final EprelConnector eprelConnector;
  public EprelResult validateRecords(List<CSVRecord> records, Set<String> fields, String category, String orgId, String productFileId) {
    Map<String, EprelValidationRule> rules = eprelValidationConfig.getSchemas();
    if (rules == null || rules.isEmpty()) {
      throw new IllegalArgumentException("No validation rules found");
    }

    ValidationContext context = new ValidationContext(fields, category, orgId, productFileId, rules);

    List<Product> validRecords = new ArrayList<>();
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    for (CSVRecord csvRow : records) {
      validateRecord(csvRow, context, validRecords, invalidRecords, errorMessages);
    }

    return new EprelResult(validRecords, invalidRecords, errorMessages);
  }

  private void validateRecord(CSVRecord csvRow, ValidationContext context, List<Product> validRecords,
                              List<CSVRecord> invalidRecords, Map<CSVRecord, String> errorMessages) {
    EprelProduct eprelData = eprelConnector.callEprel(csvRow.get(EPREL_CODE));
    List<String> errors = new ArrayList<>();

    if (eprelData == null) {
      errors.add("Product not found in EPREL");
    } else {
      validateFields(context, eprelData, errors);
    }

    if (!errors.isEmpty()) {
      invalidRecords.add(csvRow);
      errorMessages.put(csvRow, String.join(", ", errors));
    } else {
      validRecords.add(mapEprelToProduct(csvRow, eprelData, context.getOrgId(), context.getProductFileId(), context.getCategory()));
    }
  }

  private void validateFields(ValidationContext context, EprelProduct eprelData, List<String> errors) {
    for (String field : context.getFields()) {
      EprelValidationRule rule = context.getRules().get(field);
      if (rule != null) {
        String value = eprelData.getFieldValue(field);
        if (!rule.isValid(value, eprelData.getProductGroup())) {
          errors.add(rule.getMessage());
        }
      }
    }
  }

  @Getter
  @AllArgsConstructor
  private static class ValidationContext {
    private Set<String> fields;
    private String category;
    private String orgId;
    private String productFileId;
    private Map<String, EprelValidationRule> rules;
  }

}
