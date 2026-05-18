package it.gov.pagopa.register.service.validator.external.check;

import it.gov.pagopa.register.mapper.product.MappingContext;
import it.gov.pagopa.register.mapper.product.ProductMapperStrategy;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalCheckServiceTest {

  @Test
  void validateRecords_usesConfiguredProductMapperWhenDifferentFromCategory() {
    ProductRepository productRepository = mock(ProductRepository.class);
    ExternalCheckExecutor externalCheckExecutor = mock(ExternalCheckExecutor.class);
    ProductMapperStrategy eprelMapper = mock(ProductMapperStrategy.class);

    ExternalCheckService service =
      new ExternalCheckService(
        productRepository,
        Map.of("EPREL", eprelMapper),
        externalCheckExecutor
      );

    CSVRecord csvRecord = mock(CSVRecord.class);
    CategoryConfig categoryConfig =
      new CategoryConfig(
        "EPREL_STANDARD",
        "Codice GTIN/EAN",
        List.of(),
        "EPREL"
      );

    Product product =
      Product.builder()
        .gtinCode("1234567890123")
        .category("WASHINGMACHINES")
        .build();

    when(eprelMapper.extractBusinessKey(csvRecord, categoryConfig))
      .thenReturn("1234567890123");
    when(productRepository.findByGtinCodeAndInitiativeId("1234567890123", "initiativeId"))
      .thenReturn(Optional.empty());
    when(eprelMapper.mapToProduct(
      eq(csvRecord),
      eq("WASHINGMACHINES"),
      eq("orgId"),
      eq("initiativeId"),
      eq("productFileId"),
      eq("organizationName"),
      any(MappingContext.class)
    )).thenReturn(product);

    ProductValidationResult result =
      service.validateRecords(
        List.of(csvRecord),
        "WASHINGMACHINES",
        "orgId",
        "initiativeId",
        "productFileId",
        List.of("Codice GTIN/EAN"),
        "organizationName",
        initiativeConfig(categoryConfig),
        categoryConfig,
        List.of()
      );

    assertTrue(result.getInvalidRecords().isEmpty());
    assertEquals(product, result.getValidRecords().get("1234567890123"));
  }

  private InitiativeConfig initiativeConfig(CategoryConfig categoryConfig) {
    InitiativeConfig initiativeConfig = new InitiativeConfig();
    initiativeConfig.setCategories(Map.of("WASHINGMACHINES", categoryConfig));
    initiativeConfig.setExternalCheckTemplates(Map.of());
    return initiativeConfig;
  }
}
