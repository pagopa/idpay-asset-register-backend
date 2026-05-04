package it.gov.pagopa.register.service.validator.external.check;

import it.gov.pagopa.register.configuration.initiative.*;
import it.gov.pagopa.register.configuration.initiative.mapper.ProductMapperStrategy;
import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
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
      CategoryConfig categoryConfig
  ) {

    ProductMapperStrategy mapper = mapperByCategory.get(category);

    Map<String, Product> validProducts = new LinkedHashMap<>();
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    for (CSVRecord csvRecord : records) {

      boolean skip = false;

      String key = mapper.extractBusinessKey(csvRecord, categoryConfig);
      Optional<Product> existing = productRepository.findByIdAndInitiativeId(key,initiativeId);

      if (!ValidationUtils.dbCheck(
          orgId, csvRecord, existing, invalidRecords, errorMessages)) {
        skip = true;
      }

      if (!skip && validProducts.containsKey(key)) {
        invalidRecords.add(mapper.mapToCsvRow(validProducts.remove(key), headers));
        //TODO Error Message Should Depend On Initiative
        errorMessages.put(csvRecord, AssetRegisterConstants.DUPLICATE_GTIN_EAN);
        skip = true;
      }

      if (!skip) {
        for (CategoryExternalCheck check : categoryConfig.getExternalChecks()) {

          ExternalCheckTemplate template =
              initiativeConfig.getExternalCheckTemplates().get(check.getName());

          ExternalCheckResult result =
              externalCheckExecutor.execute(
                  csvRecord, template, check.getParameters(), category);

          if (!result.isValid()) {
            invalidRecords.add(csvRecord);
            errorMessages.put(csvRecord, result.getErrorMessage());
            skip = true;
            break;
          }
        }
      }

      if (skip) {
        continue;
      }

      Product product =
          mapper.mapToProduct(
              csvRecord, category, orgId, initiativeId,
              productFileId, organizationName);

      existing.ifPresent(db -> {
        product.setFormalMotivation(db.getFormalMotivation());
        product.setStatusChangeChronology(db.getStatusChangeChronology());
      });

      validProducts.put(key, product);
    }

    return new ProductValidationResult(validProducts, invalidRecords, errorMessages);
  }
}
