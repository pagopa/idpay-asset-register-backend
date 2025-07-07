package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.config.ProductFileValidationConfig;
import it.gov.pagopa.register.constants.AssetRegisterConstant;
import it.gov.pagopa.register.dto.operation.ValidationResultDTO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static it.gov.pagopa.register.constants.AssetRegisterConstant.CATEGORIES;

@Component
@RequiredArgsConstructor
public class ProductFileValidator {

  public static final String DEFAULT_CATEGORY = "eprel";
  private final ProductFileValidationConfig validationConfig;

  public ValidationResultDTO validateFile(MultipartFile file, String category, List<String> actualHeader, int recordCount) {

    // 0. check extension
    if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(".csv")) {
      return ValidationResultDTO.ko(AssetRegisterConstant.UploadKeyConstant.EXTENSION_FILE_ERROR_KEY);
    }

    // 1. load configuration
    if(!CATEGORIES.contains(category)) {
      return ValidationResultDTO.ko(AssetRegisterConstant.UploadKeyConstant.UNKNOWN_CATEGORY_ERROR_KEY);
    }

    LinkedHashMap<String, ColumnValidationRule> columnDefinitions = validationConfig.getSchemas().getOrDefault(category.toLowerCase(), validationConfig.getSchemas().get(DEFAULT_CATEGORY));
    if (columnDefinitions == null || columnDefinitions.isEmpty()) {
      return ValidationResultDTO.ko(AssetRegisterConstant.UploadKeyConstant.UNKNOWN_CATEGORY_ERROR_KEY);
    }

    // 2. retrieve excepted header
    List<String> expectedHeader = new ArrayList<>(columnDefinitions.keySet());

    // 3. check excepted header
    if (!actualHeader.equals(expectedHeader)) {
      return ValidationResultDTO.ko(AssetRegisterConstant.UploadKeyConstant.HEADER_FILE_ERROR_KEY);
    }

    // 4. check record count in file
    if (recordCount == 1) {
      return ValidationResultDTO.ko(AssetRegisterConstant.UploadKeyConstant.EMPTY_FILE_ERROR_KEY);
    }

    // 5. check if record in file exceed the max expected
    if (recordCount > validationConfig.getMaxRows()) {
      return ValidationResultDTO.ko(AssetRegisterConstant.UploadKeyConstant.MAX_ROW_FILE_ERROR_KEY);
    }

    return ValidationResultDTO.ok();

  }

  public ValidationResultDTO validateRecords(List<CSVRecord> records, List<String> headers, String category) {


    Map<String, ColumnValidationRule> rules = validationConfig.getSchemas().getOrDefault(category.toLowerCase(), validationConfig.getSchemas().get(DEFAULT_CATEGORY));
    if (rules == null || rules.isEmpty()) {
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
          if (!rule.isValid(value,category)) {
            errors.add(rule.getMessage());
          }
        }
      }

      if (!errors.isEmpty()) {
        invalidRecords.add(csvRow);
        errorMessages.put(csvRow, String.join(", ", errors));
      }
    }

    return new ValidationResultDTO(invalidRecords, errorMessages);
  }
}




