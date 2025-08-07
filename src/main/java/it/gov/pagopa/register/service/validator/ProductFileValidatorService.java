package it.gov.pagopa.register.service.validator;

import it.gov.pagopa.register.configuration.ProductFileValidationConfig;
import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.dto.operation.ValidationResultDTO;
import it.gov.pagopa.register.dto.utils.ColumnValidationRule;
import it.gov.pagopa.register.utils.CsvUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.UploadKeyConstant.REPORT_FORMAL_FILE_ERROR_KEY;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductFileValidatorService {

  private static final String DEFAULT_CATEGORY = "eprel";
  private final ProductFileValidationConfig validationConfig;

  public ValidationResultDTO validateFile(MultipartFile file, String category) throws IOException {
    String filename = Objects.requireNonNull(file.getOriginalFilename());
    log.info("[VALIDATE_FILE] - Validating file: {}, category: {}", filename, category);

    if (!filename.endsWith(CSV)) {
      log.error("[VALIDATE_FILE] - Invalid file extension: {}", filename);
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.EXTENSION_FILE_ERROR_KEY);
    }

    long fileSize = file.getSize();
    if (fileSize == 0) {
      log.warn("[VALIDATE_FILE] - File is empty: {}", filename);
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.EMPTY_FILE_ERROR_KEY);
    }

    if (fileSize > validationConfig.getMaxSize()) {
      log.error("[VALIDATE_FILE] - File size exceeds limit: {}", filename);
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.MAX_SIZE_FILE_ERROR_KEY);
    }

    if (!CATEGORIES.contains(category)) {
      log.error("[VALIDATE_FILE] - Unknown category: {}", category);
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.UNKNOWN_CATEGORY_ERROR_KEY);
    }

    Map<String, ColumnValidationRule> columnDefinitions = getColumnDefinitions(category);
    if (columnDefinitions.isEmpty()) {
      log.error("[VALIDATE_FILE] - No column definitions for category: {}", category);
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.UNKNOWN_CATEGORY_ERROR_KEY);
    }

    List<String> expectedHeader = new ArrayList<>(columnDefinitions.keySet());
    List<String> actualHeader = CsvUtils.readHeaders(file);

    if (!actualHeader.equals(expectedHeader)) {
      log.warn("[VALIDATE_FILE] - Header mismatch: {}", filename);
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.HEADER_FILE_ERROR_KEY);
    }

    List<CSVRecord> records = CsvUtils.readCsvRecords(file);
    if (records.isEmpty()) {
      log.warn("[VALIDATE_FILE] - No records found: {}", filename);
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.EMPTY_FILE_ERROR_KEY);
    }

    if (records.size() > validationConfig.getMaxRows()) {
      log.warn("[VALIDATE_FILE] - Too many records: {}", filename);
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.MAX_ROW_FILE_ERROR_KEY);
    }

    log.info("[VALIDATE_FILE] - File validation successful: {}", filename);
    return ValidationResultDTO.ok(records, actualHeader);
  }

  public ValidationResultDTO validateRecords(List<CSVRecord> records, List<String> headers, String category) {
    log.info("[VALIDATE_RECORDS] - Validating records for category: {}", category);

    Map<String, ColumnValidationRule> rules = getColumnDefinitions(category);
    if (rules == null  || rules.isEmpty()) {
      log.error("[VALIDATE_RECORDS] - No validation rules for category: {}", category);
      throw new IllegalArgumentException("No validation rules found for category: " + category);
    }

    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    for (CSVRecord csvRecord : records) {
      List<String> errors = validateRecord(csvRecord, headers, rules, category);
      if (!errors.isEmpty()) {
        log.warn("[VALIDATE_RECORDS] - Errors in record: {}", csvRecord);
        invalidRecords.add(csvRecord);
        errorMessages.put(csvRecord, String.join(", ", errors));
      }
    }

    log.info("[VALIDATE_RECORDS] - Validation completed. Invalid records: {}", invalidRecords.size());
    return invalidRecords.isEmpty()
      ? ValidationResultDTO.ok()
      : new ValidationResultDTO("KO", REPORT_FORMAL_FILE_ERROR_KEY, invalidRecords, errorMessages);
  }

  private Map<String, ColumnValidationRule> getColumnDefinitions(String category) {
    return validationConfig.getSchemas().getOrDefault(
      category.toLowerCase(),
      validationConfig.getSchemas().get(DEFAULT_CATEGORY)
    );
  }

  private List<String> validateRecord(CSVRecord csvRecord, List<String> headers, Map<String, ColumnValidationRule> rules, String category) {
    List<String> errors = new ArrayList<>();
    for (String header : headers) {
      ColumnValidationRule rule = rules.get(header);
      if (rule != null) {
        String value = csvRecord.get(header);
        if (!rule.isValid(value, CATEGORIES_TO_IT_S.get(category))) {
          errors.add(rule.getMessage().replace("{}", CATEGORIES_TO_IT_S.get(category)));
        }
      }
    }
    return errors;
  }
}
