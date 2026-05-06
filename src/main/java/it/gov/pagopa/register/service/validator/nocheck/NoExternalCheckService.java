package it.gov.pagopa.register.service.validator.nocheck;

import it.gov.pagopa.register.configuration.initiative.model.CategoryConfig;
import it.gov.pagopa.register.configuration.initiative.mapper.ProductMapperStrategy;
import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.util.*;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.DUPLICATE_GTIN_EAN;
import static it.gov.pagopa.register.utils.ValidationUtils.dbCheck;

@Component
@RequiredArgsConstructor
@Slf4j
public class NoExternalCheckService {

  private final ProductRepository productRepository;
  private final Map<String, ProductMapperStrategy> mapperByCategory;

  public ProductValidationResult validateRecords(
    List<CSVRecord> records,
    String category,
    String orgId,
    String initiativeId,
    String productFileId,
    List<String> headers,
    String organizationName,
    CategoryConfig categoryConfig,
    List<String> allowedReloadStatuses
  ) {

    ProductMapperStrategy mapper = mapperByCategory.get(category);

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

      Optional<Product> existing =
        productRepository.findByIdAndInitiativeId(businessKey, initiativeId);

      // DB check
      if (!dbCheck(orgId, csvRecord, existing, invalidRecords, errorMessages, allowedReloadStatuses)) {
        isValidRecord = false;
      }

      // Duplicate in file check
      if (isValidRecord && validProducts.containsKey(businessKey)) {

        Product duplicate = validProducts.remove(businessKey);

        CSVRecord duplicateRow =
          mapper.mapToCsvRow(duplicate, headers);

        invalidRecords.add(duplicateRow);
        // TODO generic error key
        errorMessages.put(duplicateRow, DUPLICATE_GTIN_EAN);

        isValidRecord = false;
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
            null
          );

        existing.ifPresent(dbProduct -> {
          product.setFormalMotivation(dbProduct.getFormalMotivation());
          product.setStatusChangeChronology(
            dbProduct.getStatusChangeChronology());
        });

        validProducts.put(businessKey, product);
      }
    }

    return new ProductValidationResult(
      validProducts,
      invalidRecords,
      errorMessages
    );
  }
}
