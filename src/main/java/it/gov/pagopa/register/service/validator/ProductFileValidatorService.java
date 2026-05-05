package it.gov.pagopa.register.service.validator;

import it.gov.pagopa.register.configuration.ProductFileValidationConfig;
import it.gov.pagopa.register.configuration.initiative.*;
import it.gov.pagopa.register.configuration.initiative.model.*;
import it.gov.pagopa.register.dto.operation.ValidationResultDTO;
import it.gov.pagopa.register.service.validator.rule.csv.CsvRuleContext;
import it.gov.pagopa.register.service.validator.rule.RuleDispatcher;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
import it.gov.pagopa.register.utils.CsvUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.CSV;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.UploadKeyConstant.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductFileValidatorService {

  private final ProductFileValidationConfig validationConfig;

  private final InitiativeConfigMap initiativeConfigMap;

  private final RuleDispatcher ruleDispatcher;

  public ValidationResultDTO validateFile(
    MultipartFile file,
    String category,
    String initiativeId
  ) throws IOException {

    String filename = Objects.requireNonNull(file.getOriginalFilename());
    log.info("[VALIDATE_FILE] - Validating file: {}, category: {}", filename, category);

    if (!filename.endsWith(CSV)) {
      log.error("[VALIDATE_FILE] - Invalid file extension: {}", filename);
      return ValidationResultDTO.ko(EXTENSION_FILE_ERROR_KEY);
    }

    long fileSize = file.getSize();
    if (fileSize == 0) {
      log.warn("[VALIDATE_FILE] - File is empty: {}", filename);
      return ValidationResultDTO.ko(EMPTY_FILE_ERROR_KEY);
    }

    if (fileSize > validationConfig.getMaxSize()) {
      log.error("[VALIDATE_FILE] - File size exceeds limit: {}", filename);
      return ValidationResultDTO.ko(MAX_SIZE_FILE_ERROR_KEY);
    }

    InitiativeConfig initiativeConfig = initiativeConfigMap.get(initiativeId);
    if (initiativeConfig == null) {
      log.error("[VALIDATE_FILE] - Unknown initiative: {}", initiativeId);
      return ValidationResultDTO.ko("initiative.invalid");
    }

    if (!initiativeConfig.getCategories().containsKey(category)) {
      log.error("[VALIDATE_FILE] - Unknown category: {}", category);
      return ValidationResultDTO.ko(UNKNOWN_CATEGORY_ERROR_KEY);
    }

    CategoryConfig categoryConfig = initiativeConfig.getCategories().get(category);
    if (categoryConfig == null || categoryConfig.getCsvTemplate() == null) {
      log.error("[VALIDATE_FILE] - No CSV template for category: {}", category);
      return ValidationResultDTO.ko(UNKNOWN_CATEGORY_ERROR_KEY);
    }

    CsvTemplate csvTemplate =
      initiativeConfig.getCsvTemplates().get(categoryConfig.getCsvTemplate());

    if (csvTemplate == null) {
      log.error("[VALIDATE_FILE] - CSV template not found for category: {}", category);
      return ValidationResultDTO.ko(UNKNOWN_CATEGORY_ERROR_KEY);
    }

    List<String> expectedHeaders =
      csvTemplate.getHeaders()
        .stream()
        .map(CsvHeader::getName)
        .toList();

    List<String> actualHeaders = CsvUtils.readHeaders(file);

    if (!actualHeaders.equals(expectedHeaders)) {
      log.warn("[VALIDATE_FILE] - Header mismatch: {}", filename);
      return ValidationResultDTO.ko(HEADER_FILE_ERROR_KEY);
    }

    List<CSVRecord> records = CsvUtils.readCsvRecords(file);
    if (records.isEmpty()) {
      log.warn("[VALIDATE_FILE] - No records found: {}", filename);
      return ValidationResultDTO.ko(EMPTY_FILE_ERROR_KEY);
    }

    if (records.size() > validationConfig.getMaxRows()) {
      log.warn("[VALIDATE_FILE] - Too many records: {}", filename);
      return ValidationResultDTO.ko(MAX_ROW_FILE_ERROR_KEY);
    }

    log.info("[VALIDATE_FILE] - File validation successful: {}", filename);
    return ValidationResultDTO.ok(records, actualHeaders);
  }

  public ValidationResultDTO validateRecords(
    List<CSVRecord> records,
    String category,
    String initiativeId
  ) {

    log.info("[VALIDATE_RECORDS] - Validating records for category: {}", category);

    InitiativeConfig initiativeConfig = initiativeConfigMap.get(initiativeId);
    CategoryConfig categoryConfig = initiativeConfig.getCategories().get(category);
    CsvTemplate csvTemplate =
      initiativeConfig.getCsvTemplates().get(categoryConfig.getCsvTemplate());

    if (csvTemplate.getRules() == null ||
      csvTemplate.getRules().isEmpty()) {
      log.error("[VALIDATE_RECORDS] - No validation rules for category: {}", category);
      throw new IllegalArgumentException(
        "No validation rules found for category: " + category
      );
    }

    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    for (CSVRecord csvRecord : records) {

      List<String> errors =
        validateRecord(csvRecord, csvTemplate, category);

      if (!errors.isEmpty()) {
        log.warn("[VALIDATE_RECORDS] - Errors in record: {}", csvRecord);
        invalidRecords.add(csvRecord);
        errorMessages.put(csvRecord, String.join(", ", errors));
      }
    }

    log.info("[VALIDATE_RECORDS] - Validation completed. Invalid records: {}", invalidRecords.size());

    return invalidRecords.isEmpty()
      ? ValidationResultDTO.ok()
      : new ValidationResultDTO(
      "KO",
      REPORT_FORMAL_FILE_ERROR_KEY,
      invalidRecords,
      errorMessages
    );
  }

  /**
   * Valida una singola riga CSV usando il CsvTemplate
   */
  private List<String> validateRecord(
    CSVRecord csvRecord,
    CsvTemplate csvTemplate,
    String category
  ) {

    List<String> errors = new ArrayList<>();

    List<ValidationRule> rules =
        csvTemplate.getRules();

      if (rules == null || rules.isEmpty()) {
        return errors;
      }

      for (ValidationRule rule : rules) {

        RuleExecutor ruleExecutor =
          ruleDispatcher.resolve(rule.getType());

        CsvRuleContext csvRuleContext =
          new CsvRuleContext(csvRecord,category);

        boolean valid = ruleExecutor.evaluate(
          rule,
          csvRuleContext
        );

        if (!valid) {
          errors.add(
            ruleExecutor.errorMessage(
              rule,
              csvRuleContext
             )
          );
        }
      }

    return errors;
  }

}
