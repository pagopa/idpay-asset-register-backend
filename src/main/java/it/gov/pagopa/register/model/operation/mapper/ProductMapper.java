package it.gov.pagopa.register.model.operation.mapper;

import it.gov.pagopa.register.utils.EprelProduct;
import it.gov.pagopa.register.model.operation.Product;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDateTime;

import static it.gov.pagopa.register.constants.RegisterConstants.CsvRecord.*;
import static it.gov.pagopa.register.constants.RegisterConstants.CsvRecord.PRODUCTION_COUNTRY;

public class ProductMapper {

  private ProductMapper(){}
  public static Product mapCookingHobToProduct(CSVRecord csvRecord, String orgId, String productFileId) {
    return Product.builder()
      .productFileId(productFileId)
      .organizationId(orgId)
      .registrationDate(LocalDateTime.now())
      .status(STATUS_APPROVED)
      .productCode(csvRecord.get(PRODUCT_CODE))
      .gtinCode(csvRecord.get(GTIN_EAN_CODE))
      .category(CATEGORY_COOKINGHOBS)
      .countryOfProduction(csvRecord.get(PRODUCTION_COUNTRY))
      .brand(csvRecord.get(BRAND))
      .model(csvRecord.get(MODEL))
      .build();
  }

  public static Product mapEprelToProduct(CSVRecord csvRecord, EprelProduct eprelData, String orgId, String productFileId, String category) {
    return Product.builder()
      .productFileId(productFileId)
      .organizationId(orgId)
      .registrationDate(LocalDateTime.now())
      .status(STATUS_APPROVED)
      .productCode(csvRecord.get(PRODUCT_CODE))
      .gtinCode(csvRecord.get(GTIN_EAN_CODE))
      .eprelCode(csvRecord.get(EPREL_CODE))
      .category(category)
      .productGroup(eprelData.getProductGroup())
      .countryOfProduction(csvRecord.get(PRODUCTION_COUNTRY))
      .brand(eprelData.getSupplierOrTrademark())
      .model(eprelData.getModelIdentifier())
      .energyClass(eprelData.getEnergyClass())
      .build();
  }
}
