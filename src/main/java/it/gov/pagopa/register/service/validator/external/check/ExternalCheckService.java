package it.gov.pagopa.register.service.validator.external.check;

import it.gov.pagopa.register.mapper.product.MappingContext;
import it.gov.pagopa.register.mapper.product.ProductMapperStrategy;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.initiative.CategoryExternalCheck;
import it.gov.pagopa.register.model.initiative.ExternalCheckTemplate;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
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
public class ExternalCheckService {

  private final ProductRepository productRepository;
  private final Map<String, ProductMapperStrategy> mapperByCategory;
  private final ExternalCheckExecutor externalCheckExecutor;

  public ProductValidationResult validateRecords(
      List<CSVRecord> records,
      String category,
      String orgId,
      String initiativeId,
      String productFileId,
      List<String> headers,
      String organizationName,
      InitiativeConfig initiativeConfig,
      CategoryConfig categoryConfig,
      List<String> allowedReloadStatuses
  ) {

    ProductMapperStrategy mapper = mapperByCategory.get(initiativeConfig.getCategories().get(category).getProductMapper());

    Map<String, Product> validProducts = new LinkedHashMap<>();
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    for (CSVRecord csvRecord : records) {

      boolean isValidRecord = true;

      String businessKey = mapper.extractBusinessKey(csvRecord, categoryConfig);
      Optional<Product> existing =
        productRepository.findByGtinCodeAndInitiativeId(businessKey, initiativeId);

      if (!dbCheck(
        orgId, csvRecord, existing, invalidRecords, errorMessages, allowedReloadStatuses)) {
        isValidRecord = false;
      }

      if (isValidRecord && validProducts.containsKey(businessKey)) {

        Product duplicate = validProducts.remove(businessKey);

        CSVRecord duplicateRow =
          mapper.mapToCsvRow(duplicate, headers);

        invalidRecords.add(duplicateRow);

        // NOTE: DUPLICATE_GTIN_EAN assumes GTIN/EAN as the unique key.
        // This will not be valid if products without GTIN/EAN as identifier are introduce

        errorMessages.put(duplicateRow, DUPLICATE_GTIN_EAN);
        isValidRecord = false;
      }

      Map<String, Object> externalData = new HashMap<>();

      if (isValidRecord) {

        for (CategoryExternalCheck check : categoryConfig.getExternalChecks()) {

          ExternalCheckTemplate template =
            initiativeConfig.getExternalCheckTemplates()
              .get(check.getKey());

          ExternalCheckResult checkResult =
            externalCheckExecutor.execute(
              csvRecord,
              template,
              check.getParameters(),
              category
            );

          if (!checkResult.isValid()) {
            invalidRecords.add(csvRecord);
            errorMessages.put(csvRecord, checkResult.getErrorMessage());
            isValidRecord = false;
          }
          externalData.putAll(checkResult.getExternalData());
        }
      }
      if (isValidRecord) {

        MappingContext mappingContext =
          new MappingContext(externalData);

        Product product =
          mapper.mapToProduct(
            csvRecord,
            category,
            orgId,
            initiativeId,
            productFileId,
            organizationName,
            mappingContext
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
}
