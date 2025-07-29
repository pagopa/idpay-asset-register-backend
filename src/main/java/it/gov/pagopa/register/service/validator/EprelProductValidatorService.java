package it.gov.pagopa.register.service.validator;

import it.gov.pagopa.register.configuration.EprelValidationConfig;
import it.gov.pagopa.register.connector.eprel.EprelConnector;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.dto.utils.EprelResult;
import it.gov.pagopa.register.dto.utils.EprelValidationRule;
import it.gov.pagopa.register.enums.ProductStatusEnum;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.util.*;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EprelProductValidatorService {


  private final EprelValidationConfig eprelValidationConfig;
  private final EprelConnector eprelConnector;
  private final ProductRepository productRepository;
  public EprelResult validateRecords(List<CSVRecord> records, Set<String> fields, String category, String orgId, String productFileId, List<String> headers) {
    log.info("[VALIDATE_RECORDS] - Validating records for organizationId: {}, category: {}, productFileId: {}", orgId, category, productFileId);
    Map<String, EprelValidationRule> rules = eprelValidationConfig.getSchemas();
    if (rules == null || rules.isEmpty()) {
      log.error("[VALIDATE_RECORDS] - No validation rules found");
      throw new IllegalArgumentException("No validation rules found");
    }

    ValidationContext context = new ValidationContext(fields, category, orgId, productFileId, headers, rules);

    Map<String, Product> validRecords = new LinkedHashMap<>();
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    for (CSVRecord csvRow : records) {

      validateRecord(csvRow, context, validRecords, invalidRecords, errorMessages);
    }

    log.info("[VALIDATE_RECORDS] - Validation completed. Valid records: {}, Invalid records: {}", validRecords.size(), invalidRecords.size());
    return new EprelResult(validRecords, invalidRecords, errorMessages);
  }

  private void validateRecord(CSVRecord csvRow, ValidationContext context,
                              Map<String, Product> validRecords,
                              List<CSVRecord> invalidRecords,
                              Map<CSVRecord, String> errorMessages) {

    Optional<Product> optProduct = productRepository.findById(csvRow.get(CODE_GTIN_EAN));
    boolean isProductPresent = optProduct.isPresent();
    if(isProductPresent){
      if( !context.getOrgId().equals(optProduct.get().getOrganizationId())){
        invalidRecords.add(csvRow);
        errorMessages.put(csvRow,DIFFERENT_ORGANIZATIONID);
        return;
      } else if (!ProductStatusEnum.APPROVED.toString().equals(optProduct.get().getStatus())) {
        invalidRecords.add(csvRow);
        errorMessages.put(csvRow, STATUS_NOT_APPROVED);
        return;
      }
    }

    log.info("[VALIDATE_RECORD] - Validating record with EPREL code: {}", csvRow.get(CODE_EPREL));
    EprelProduct eprelData = eprelConnector.callEprel(csvRow.get(CODE_EPREL));
    log.info("[VALIDATE_RECORD] - EPREL response: {}", eprelData);

    if (eprelData == null) {
      log.warn("[VALIDATE_RECORD] - Product not found in EPREL for code: {}", csvRow.get(CODE_EPREL));
      invalidRecords.add(csvRow);
      errorMessages.put(csvRow, "Product not found in EPREL");
      return;
    }

    if (WASHERDRIERS.equalsIgnoreCase(context.getCategory())) {
      eprelData.setEnergyClass(eprelData.getEnergyClassWash());
    }

    List<String> errors = new ArrayList<>();
    validateFields(context, eprelData, errors);

    if (!errors.isEmpty()) {
      log.warn("[VALIDATE_RECORD] - Validation errors for record with EPREL code: {}: {}", csvRow.get(CODE_EPREL), String.join(", ", errors));
      invalidRecords.add(csvRow);
      errorMessages.put(csvRow, String.join(", ", errors));
      return;
    }

    log.info("[VALIDATE_RECORD] - EPREL product valid: {}", csvRow.get(CODE_EPREL));
    String gtin = csvRow.get(CODE_GTIN_EAN);

    if(validRecords.containsKey(csvRow.get(CODE_GTIN_EAN))) {
      Product duplicateGtin = validRecords.remove(csvRow.get(CODE_GTIN_EAN));
      CSVRecord duplicateGtinRow = mapProductToCsvRow(duplicateGtin,context.getCategory(), context.getHeaders());
      invalidRecords.add(duplicateGtinRow);
      errorMessages.put(duplicateGtinRow,DUPLICATE_GTIN_EAN);

      log.warn("[VALIDATE_RECORD] - Duplicate error for record with GTIN code: {}", gtin);
    }
    Product product = mapEprelToProduct(csvRow, eprelData, context.getOrgId(), context.getProductFileId(), context.getCategory());
    validRecords.put(gtin, product);
    log.info("[PRODUCT_UPLOAD] - Added eprel product: {}", csvRow.get(CODE_GTIN_EAN));
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
    private List<String> headers;
    private Map<String, EprelValidationRule> rules;
  }

}
