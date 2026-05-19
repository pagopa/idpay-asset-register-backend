package it.gov.pagopa.register.service.validator.product;

import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.mapper.product.MappingContext;
import it.gov.pagopa.register.mapper.product.ProductMapperStrategy;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;

import java.util.*;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.DUPLICATE_GTIN_EAN;
import static it.gov.pagopa.register.utils.ValidationUtils.dbCheck;

@Slf4j
@SuppressWarnings("java:S107")
public abstract class AbstractValidationService {

  protected final ProductRepository productRepository;
  protected final Map<String, ProductMapperStrategy> mapperByCategory;

  protected AbstractValidationService(
      ProductRepository productRepository,
      Map<String, ProductMapperStrategy> mapperByCategory) {
    this.productRepository = productRepository;
    this.mapperByCategory = mapperByCategory;
  }

  protected ProductValidationResult validateInternal(
      List<CSVRecord> records,
      String category,
      String orgId,
      String initiativeId,
      String productFileId,
      List<String> headers,
      String organizationName,
      CategoryConfig categoryConfig,
      List<String> allowedReloadStatuses,
      ExternalContext externalContext
  ) {

    ProductMapperStrategy mapper =
        mapperByCategory.get(categoryConfig.getProductMapper());

    if (mapper == null) {
      throw new IllegalStateException(
          "No ProductMapperStrategy configured for category: " + category);
    }

    Map<String, Product> validProducts = new LinkedHashMap<>();
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    for (CSVRecord csvRecord : records) {

      boolean isValidRecord = true;

      String businessKey = mapper.extractBusinessKey(csvRecord, categoryConfig);

      // NOTE: Assumes GTIN/EAN as the unique key.
      // This will not be valid if products without GTIN/EAN as identifier are introduced.
      Optional<Product> existing =
          productRepository.findByGtinCodeAndInitiativeId(businessKey, initiativeId);

      // DB check
      if (!dbCheck(
          orgId, csvRecord, existing, invalidRecords, errorMessages, allowedReloadStatuses)) {
        isValidRecord = false;
      }

      // Duplicate check
      if (isValidRecord && handleDuplicate(
          businessKey, validProducts, mapper, headers, invalidRecords, errorMessages)) {
          isValidRecord = false;
      }

      // External checks (hook)
      Map<String, Object> externalData = Collections.emptyMap();
      if (isValidRecord) {
        externalData = performExternalChecks(csvRecord, externalContext);

        if (externalData.isEmpty()) {
          invalidRecords.add(csvRecord);
          errorMessages.put(csvRecord, "EXTERNAL_CHECK_FAILED");
          isValidRecord = false;
        }
      }

      if (isValidRecord) {

        Product product =
            mapper.mapToProduct(
                csvRecord,
                category,
                orgId,
                initiativeId,
                productFileId,
                organizationName,
                new MappingContext(externalData)
            );

        existing.ifPresent(db -> {
          product.setFormalMotivation(db.getFormalMotivation());
          product.setStatusChangeChronology(db.getStatusChangeChronology());
        });

        validProducts.put(businessKey, product);
      }
    }

    return new ProductValidationResult(validProducts, invalidRecords, errorMessages);
  }

  protected boolean handleDuplicate(
      String businessKey,
      Map<String, Product> validProducts,
      ProductMapperStrategy mapper,
      List<String> headers,
      List<CSVRecord> invalidRecords,
      Map<CSVRecord, String> errorMessages
  ) {

    if (!validProducts.containsKey(businessKey)) {
      return false;
    }

    Product duplicate = validProducts.remove(businessKey);

    CSVRecord duplicateRow = mapper.mapToCsvRow(duplicate, headers);

    invalidRecords.add(duplicateRow);

    // NOTE: DUPLICATE_GTIN_EAN assumes GTIN/EAN as the unique key.
    // This will not be valid if products without GTIN/EAN as identifier are introduced.
    errorMessages.put(duplicateRow, DUPLICATE_GTIN_EAN);

    return true;
  }

  protected abstract Map<String, Object> performExternalChecks(
      CSVRecord csvRecord,
      ExternalContext context
  );
}
