package it.gov.pagopa.register.service.validator;

import it.gov.pagopa.register.configuration.EprelValidationConfig;
import it.gov.pagopa.register.connector.eprel.EprelConnector;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.dto.utils.EprelValidationRule;
import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.exception.operation.EprelException;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.mapEprelToProduct;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.mapProductToCsvRow;
import static it.gov.pagopa.register.utils.ValidationUtils.addError;
import static it.gov.pagopa.register.utils.ValidationUtils.dbCheck;

@Component
@RequiredArgsConstructor
@Slf4j
public class EprelProductValidatorService {

  private final EprelValidationConfig eprelValidationConfig;
  private final EprelConnector eprelConnector;
  private final ProductRepository productRepository;

  public ProductValidationResult validateRecords(
    List<CSVRecord> records,
    Set<String> fields,
    String category,
    String orgId,
    String productFileId,
    List<String> headers,
    String organizationName) {

    log.info("[VALIDATE_RECORDS] - Validating records for organizationId: {}, category: {}, productFileId: {}", orgId, category, productFileId);

    Map<String, EprelValidationRule> rules = eprelValidationConfig.getSchemas();
    if (rules == null || rules.isEmpty()) {
      log.error("[VALIDATE_RECORDS] - No validation rules found");
      throw new IllegalArgumentException("No validation rules found");
    }

    ValidationContext context = new ValidationContext(fields, category, orgId, productFileId, headers, rules, organizationName);

    Map<String, Product> validRecords = new LinkedHashMap<>();
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    for (CSVRecord csvRecord : records) {
      validateRecord(csvRecord, context, validRecords, invalidRecords, errorMessages);
    }

    log.info("[VALIDATE_RECORDS] - Validation completed. Valid: {}, Invalid: {}", validRecords.size(), invalidRecords.size());
    return new ProductValidationResult(validRecords, invalidRecords, errorMessages);
  }

  private void validateRecord(
    CSVRecord csvRecord,
    ValidationContext context,
    Map<String, Product> validRecords,
    List<CSVRecord> invalidRecords,
    Map<CSVRecord, String> errorMessages) {

    String gtin = csvRecord.get(CODE_GTIN_EAN);
    String eprelCode = csvRecord.get(CODE_EPREL);

    Optional<Product> existingProduct = productRepository.findById(gtin);
    boolean dbCheck = dbCheck(context.orgId, csvRecord, existingProduct, invalidRecords, errorMessages);
    if(!dbCheck) {
      return;
    }

    log.info("[VALIDATE_RECORD] - Validating EPREL code: {}", eprelCode);
    EprelProduct eprelData;
    try {
      eprelData = eprelConnector.callEprel("TEST");
      log.info("[VALIDATE_RECORD] - EPREL response: {}", eprelData);
    } catch (HttpClientErrorException e) {
      addError(csvRecord, "EPREL client error", invalidRecords, errorMessages);
      return;
    } catch (HttpServerErrorException | ResourceAccessException e) {
      throw new EprelException("EPREL server error: " + e.getMessage());
    }

    if (WASHERDRIERS.equalsIgnoreCase(context.getCategory())) {
      eprelData.setEnergyClass(eprelData.getEnergyClassWash());
    }


    List<String> errors = validateFields(context, eprelData);
    if (!errors.isEmpty()) {
      addError(csvRecord, String.join(", ", errors), invalidRecords, errorMessages);
      return;
    }

    if (validRecords.containsKey(gtin)) {
      Product duplicate = validRecords.remove(gtin);
      CSVRecord duplicateRow = mapProductToCsvRow(duplicate, context.getCategory(), context.getHeaders());
      addError(duplicateRow, DUPLICATE_GTIN_EAN, invalidRecords, errorMessages);
      log.warn("[VALIDATE_RECORD] - Duplicate GTIN: {}", gtin);
    }

    Product product = mapEprelToProduct(csvRecord, eprelData, context.getOrgId(), context.getProductFileId(), context.getCategory(), context.getOrganizationName());
    validRecords.put(gtin, product);
    log.info("[PRODUCT_UPLOAD] - Added product: {}", gtin);
  }

  private List<String> validateFields(ValidationContext context, EprelProduct eprelData) {
    List<String> errors = new ArrayList<>();
    for (String field : context.getFields()) {
      EprelValidationRule rule = context.getRules().get(field);
      if (rule != null) {
        String value = eprelData.getFieldValue(field);
        if (!rule.isValid(value, context.getCategory())) {
          errors.add(rule.getMessage());
        }
      }
    }
    return errors;
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
    private String organizationName;
  }
}
