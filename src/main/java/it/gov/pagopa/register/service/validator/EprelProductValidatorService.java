package it.gov.pagopa.register.service.validator;

import it.gov.pagopa.register.config.EprelValidationConfig;
import it.gov.pagopa.register.connector.eprel.EprelConnector;
import it.gov.pagopa.register.utils.EprelProduct;
import it.gov.pagopa.register.utils.EprelResult;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.utils.EprelValidationRule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.util.*;


import static it.gov.pagopa.register.constants.AssetRegisterConstants.CODE_EPREL;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.WASHERDRIERS;
import static it.gov.pagopa.register.model.operation.mapper.ProductMapper.mapEprelToProduct;

@Service
@RequiredArgsConstructor
@Slf4j
public class EprelProductValidatorService {


  private final EprelValidationConfig eprelValidationConfig;
  private final EprelConnector eprelConnector;
  public EprelResult validateRecords(List<CSVRecord> records, Set<String> fields, String category, String orgId, String productFileId) {
    log.info("[VALIDATE_RECORDS] - Validating records for organizationId: {}, category: {}, productFileId: {}", orgId, category, productFileId);
    Map<String, EprelValidationRule> rules = eprelValidationConfig.getSchemas();
    if (rules == null || rules.isEmpty()) {
      log.error("[VALIDATE_RECORDS] - No validation rules found");
      throw new IllegalArgumentException("No validation rules found");
    }

    ValidationContext context = new ValidationContext(fields, category, orgId, productFileId, rules);

    List<Product> validRecords = new ArrayList<>();
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    for (CSVRecord csvRow : records) {
      validateRecord(csvRow, context, validRecords, invalidRecords, errorMessages);
    }

    log.info("[VALIDATE_RECORDS] - Validation completed. Valid records: {}, Invalid records: {}", validRecords.size(), invalidRecords.size());
    return new EprelResult(validRecords, invalidRecords, errorMessages);
  }

  private void validateRecord(CSVRecord csvRow, ValidationContext context, List<Product> validRecords,
                              List<CSVRecord> invalidRecords, Map<CSVRecord, String> errorMessages) {
    log.info("[VALIDATE_RECORD] - Validating record with EPREL code: {}", csvRow.get(CODE_EPREL));
    EprelProduct eprelData = eprelConnector.callEprel(csvRow.get(CODE_EPREL));

    List<String> errors = new ArrayList<>();

    if (eprelData == null) {
      log.warn("[VALIDATE_RECORD] - Product not found in EPREL for code: {}", csvRow.get(CODE_EPREL));
      errors.add("Product not found in EPREL");
    } else {
      if (WASHERDRIERS.equalsIgnoreCase(context.getCategory())) {
        eprelData.setEnergyClass(eprelData.getEnergyClassWash());
      }
      log.info("[VALIDATE_RECORD] - EPREL response for {}: {}", eprelData.getEprelRegistrationNumber(), eprelData);
      validateFields(context, eprelData, errors);
    }

    if (!errors.isEmpty()) {
      log.warn("[VALIDATE_RECORD] - Validation errors for record with EPREL code: {}: {}", csvRow.get(CODE_EPREL), String.join(", ", errors));
      invalidRecords.add(csvRow);
      errorMessages.put(csvRow, String.join(", ", errors));
    } else {
      log.info("[VALIDATE_RECORD] - EPREL product valid: {}", eprelData.getEprelRegistrationNumber());
      validRecords.add(mapEprelToProduct(csvRow, eprelData, context.getOrgId(), context.getProductFileId(), context.getCategory()));
    }
  }

  private void validateFields(ValidationContext context, EprelProduct eprelData, List<String> errors) {
    for (String field : context.getFields()) {
      EprelValidationRule rule = context.getRules().get(field);
      if (rule != null) {
        String value = eprelData.getFieldValue(field);
        if (!rule.isValid(value, context.getCategory())) {
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
