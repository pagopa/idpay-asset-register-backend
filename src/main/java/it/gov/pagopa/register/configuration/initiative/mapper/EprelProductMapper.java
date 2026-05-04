package it.gov.pagopa.register.configuration.initiative.mapper;

import it.gov.pagopa.register.configuration.initiative.CategoryConfig;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.mapper.operation.ProductMapper;
import it.gov.pagopa.register.model.operation.Product;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.limitName;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.normalizeCsvCode;

@Component("EPREL")
public class EprelProductMapper implements ProductMapperStrategy {

  @Override
  public String extractBusinessKey(
      CSVRecord record,
      CategoryConfig categoryConfig
  ) {

    return normalizeCsvCode(record.get(categoryConfig.getInputIdentifierField()));
  }

  @Override
  public Product mapToProduct(
      CSVRecord record,
      String category,
      String orgId,
      String initiativeId,
      String productFileId,
      String organizationName
  ) {

    String gtinCode = normalizeCsvCode(record.get(CODE_GTIN_EAN));
    String eprelCode = normalizeCsvCode(record.get(CODE_EPREL));
    String productCode = normalizeCsvCode(record.get(CODE_PRODUCT));

    String productName = limitName(
        CATEGORIES_TO_IT_S.get(category) + " " +
        record.get(BRAND) + " " +
        record.get(MODEL)
    );

    String fullProductName = limitName(
        gtinCode + " - " + productName
    );

    return Product.builder()
        .productFileId(productFileId)
        .organizationId(orgId)
        .registrationDate(LocalDateTime.now(ZoneOffset.UTC))
        .status(ProductStatus.UPLOADED.name())
        .initiativeId(initiativeId)

        .gtinCode(gtinCode)
        .eprelCode(eprelCode)
        .productCode(productCode)
        .category(category)

        .brand(record.get(BRAND))
        .model(record.get(MODEL))
        .countryOfProduction(record.get(COUNTRY_OF_PRODUCTION))

        .productName(productName)
        .fullProductName(fullProductName)

        .organizationName(organizationName)
        .statusChangeChronology(new ArrayList<>())
        .formalMotivation("")
        .build();
  }

  @Override
  public CSVRecord mapToCsvRow(Product product, List<String> headers) {
    return ProductMapper.mapProductToCsvRow(product, product.getCategory(), headers);
  }
}
