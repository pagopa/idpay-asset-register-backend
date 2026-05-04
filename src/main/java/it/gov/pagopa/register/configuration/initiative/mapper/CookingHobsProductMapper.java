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

@Component("COOKINGHOBS")
public class CookingHobsProductMapper implements ProductMapperStrategy {

  @Override
  public String extractBusinessKey(CSVRecord record, CategoryConfig config) {
    return record.get(config.getInputIdentifierField());
  }


  @Override
  public Product mapToProduct(
    CSVRecord csvRecord,
    String category,
    String orgId,
    String initiativeId,
    String productFileId,
    String organizationName
  ) {

    String codeProduct = normalizeCsvCode(csvRecord.get(CODE_PRODUCT));
    String gtinCode = normalizeCsvCode(csvRecord.get(CODE_GTIN_EAN));

    String productName = CATEGORIES_TO_IT_S.get(COOKINGHOBS) + " " + csvRecord.get(BRAND) + " " + csvRecord.get(MODEL);
    String fullProductName = gtinCode + " - " + productName;

    return Product.builder()
      .productFileId(productFileId)
      .organizationId(orgId)
      .registrationDate(LocalDateTime.now(ZoneOffset.UTC))
      .status(ProductStatus.UPLOADED.name())
      .productCode(codeProduct)
      .gtinCode(gtinCode)
      .category(COOKINGHOBS)
      .countryOfProduction(csvRecord.get(COUNTRY_OF_PRODUCTION))
      .brand(csvRecord.get(BRAND))
      .model(csvRecord.get(MODEL))
      .capacity("N\\A")
      .productName(limitName(productName))
      .fullProductName(limitName(fullProductName))
      .organizationName(organizationName)
      .statusChangeChronology(new ArrayList<>())
      .formalMotivation("")
      .build();
  }

  @Override
  public CSVRecord mapToCsvRow(Product product, List<String> headers) {
    return ProductMapper.mapProductToCsvRow(product, COOKINGHOBS, headers);
  }
}
