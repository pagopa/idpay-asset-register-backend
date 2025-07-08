package it.gov.pagopa.register.model.operation.mapper;

import it.gov.pagopa.register.utils.EprelProduct;
import it.gov.pagopa.register.model.operation.Product;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDateTime;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;

public class ProductMapper {

  private ProductMapper(){}
  public static Product mapCookingHobToProduct(CSVRecord csvRecord, String orgId, String productFileId) {
    return Product.builder()
      .productFileId(productFileId)
      .organizationId(orgId)
      .registrationDate(LocalDateTime.now())
      .status(STATUS_APPROVED)
      .productCode(csvRecord.get(CODE_PRODUCT))
      .gtinCode(csvRecord.get(CODE_GTIN_EAN))
      .category(COOKINGHOBS)
      .countryOfProduction(csvRecord.get(COUNTRY_OF_PRODUCTION))
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
      .productCode(csvRecord.get(CODE_PRODUCT))
      .gtinCode(csvRecord.get(CODE_GTIN_EAN))
      .eprelCode(csvRecord.get(CODE_EPREL))
      .category(category)
      .productGroup(eprelData.getProductGroup())
      .countryOfProduction(csvRecord.get(COUNTRY_OF_PRODUCTION))
      .brand(eprelData.getSupplierOrTrademark())
      .model(eprelData.getModelIdentifier())
      .energyClass(eprelData.getEnergyClass())
      .build();
  }
}
