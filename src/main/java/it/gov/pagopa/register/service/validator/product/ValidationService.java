package it.gov.pagopa.register.service.validator.product;

import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.mapper.product.ProductMapperStrategy;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.initiative.CategoryExternalCheck;
import it.gov.pagopa.register.model.initiative.ExternalCheckTemplate;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.service.validator.external.system.check.ExternalCheckExecutor;
import it.gov.pagopa.register.service.validator.external.system.check.ExternalCheckResult;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ValidationService extends AbstractValidationService {

  private final ExternalCheckExecutor externalCheckExecutor;
  public ValidationService(ProductRepository productRepository, Map<String, ProductMapperStrategy> mapperByCategory, ExternalCheckExecutor externalCheckExecutor) {
    super(productRepository, mapperByCategory);
    this.externalCheckExecutor = externalCheckExecutor;
  }

  @Override
  public Map<String, Object> performExternalChecks(
    CSVRecord csvRecord,
    ExternalContext context) {

    if (context == null || context.getCategoryConfig().getExternalChecks().isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, Object> externalData = new HashMap<>();

    for (CategoryExternalCheck check : context.getCategoryConfig().getExternalChecks()) {

      ExternalCheckTemplate template =
          context.getInitiativeConfig()
              .getExternalCheckTemplates()
              .get(check.getKey());

      ExternalCheckResult result =
          externalCheckExecutor.execute(
              csvRecord,
              template,
              check.getParameters(),
              context.getCategory()
          );

      if (!result.isValid()) {
        return null;
      }

      externalData.putAll(result.getExternalData());
    }

    return externalData;
  }

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

    ExternalContext context = new ExternalContext(
        initiativeConfig,
        categoryConfig,
        externalCheckExecutor,
        category
    );

    return validateInternal(
        records,
        category,
        orgId,
        initiativeId,
        productFileId,
        headers,
        organizationName,
        categoryConfig,
        allowedReloadStatuses,
        context
    );
  }
}
