package it.gov.pagopa.register.service.validator;

import it.gov.pagopa.register.configuration.ProductFileValidationConfig;
import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.dto.operation.ValidationResultDTO;
import it.gov.pagopa.register.dto.utils.ColumnValidationRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.UploadKeyConstant.REPORT_FORMAL_FILE_ERROR_KEY;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductFileValidatorService {

  public static final String DEFAULT_CATEGORY = "eprel";
  private final ProductFileValidationConfig validationConfig;

  public ValidationResultDTO validateFile(MultipartFile file, String category, List<String> actualHeader, int recordCount) {
    log.info("[VALIDATE_FILE] - Validating file: {}, category: {}, recordCount: {}", file.getOriginalFilename(), category, recordCount);

    if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(CSV)) {
      log.error("[VALIDATE_FILE] - Invalid file extension for file: {}", file.getOriginalFilename());
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.EXTENSION_FILE_ERROR_KEY);
    }

    if (file.getSize() > CSV_SIZE) {
      log.error("[VALIDATE_FILE] - Invalid size for file: {}", file.getOriginalFilename());
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.MAX_SIZE_FILE_ERROR_KEY);
    }

    if (recordCount == 0) {
      log.warn("[VALIDATE_FILE] - File is empty: {}", file.getOriginalFilename());
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.EMPTY_FILE_ERROR_KEY);
    }

    if (!CATEGORIES.contains(category)) {
      log.error("[VALIDATE_FILE] - Unknown category: {}", category);
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.UNKNOWN_CATEGORY_ERROR_KEY);
    }

    LinkedHashMap<String, ColumnValidationRule> columnDefinitions = validationConfig.getSchemas().getOrDefault(category.toLowerCase(), validationConfig.getSchemas().get(DEFAULT_CATEGORY));
    if (columnDefinitions == null || columnDefinitions.isEmpty()) {
      log.error("[VALIDATE_FILE] - No column definitions found for category: {}", category);
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.UNKNOWN_CATEGORY_ERROR_KEY);
    }

    List<String> expectedHeader = new ArrayList<>(columnDefinitions.keySet());


    if (!actualHeader.equals(expectedHeader)) {
      log.warn("[VALIDATE_FILE] - Header mismatch for file: {}", file.getOriginalFilename());
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.HEADER_FILE_ERROR_KEY);
    }

    // 5. check if record in file exceed the max expected
    if (recordCount > validationConfig.getMaxRows()) {
      log.warn("[VALIDATE_FILE] - File exceeds maximum row count: {}", file.getOriginalFilename());
      return ValidationResultDTO.ko(AssetRegisterConstants.UploadKeyConstant.MAX_ROW_FILE_ERROR_KEY);
    }

    log.info("[VALIDATE_FILE] - File validation successful: {}", file.getOriginalFilename());
    return ValidationResultDTO.ok();
  }

  public ValidationResultDTO validateRecords(List<CSVRecord> records, List<String> headers, String category) {
    log.info("[VALIDATE_RECORDS] - Validating records for category: {}", category);

    Map<String, ColumnValidationRule> rules = validationConfig.getSchemas().getOrDefault(category.toLowerCase(), validationConfig.getSchemas().get(DEFAULT_CATEGORY));
    if (rules == null || rules.isEmpty()) {
      log.error("[VALIDATE_RECORDS] - No validation rules found for category: {}", category);
      throw new IllegalArgumentException("No validation rules found for category: " + category);
    }

    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    for (CSVRecord csvRow : records) {
      List<String> errors = new ArrayList<>();

      for (String header : headers) {
        ColumnValidationRule rule = rules.get(header);
        if (rule != null) {
          String value = csvRow.get(header);
          if (!rule.isValid(value, category)) {
            errors.add(rule.getMessage());
          }
        }
      }

      if (!errors.isEmpty()) {
        log.warn("[VALIDATE_RECORDS] - Validation errors for record: {}", csvRow);
        invalidRecords.add(csvRow);
        errorMessages.put(csvRow, String.join(", ", errors));
      }
    }

    log.info("[VALIDATE_RECORDS] - Validation completed. Invalid records: {}", invalidRecords.size());
    if(!invalidRecords.isEmpty()) {
      return new ValidationResultDTO("KO",REPORT_FORMAL_FILE_ERROR_KEY,invalidRecords, errorMessages);
    }
    return ValidationResultDTO.ok();
  }

}




