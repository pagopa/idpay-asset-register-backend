package it.gov.pagopa.register.model.operation.mapper;

import it.gov.pagopa.register.utils.EprelProduct;
import it.gov.pagopa.register.model.operation.Product;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.utils.CsvUtils.DELIMITER;
import static it.gov.pagopa.register.utils.EprelUtils.mapEnergyClass;


public class ProductMapper {

  private ProductMapper() {
  }

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
      .energyClass(mapEnergyClass(eprelData.getEnergyClass()))
      .build();
  }


  public static CSVRecord mapProductToCsvRow(Product product, String category, List<String> headers) {
    try {
      StringWriter out = new StringWriter();
      CSVPrinter printer = new CSVPrinter(out, CSVFormat.Builder.create()
        .setHeader(headers.toArray(new String[0]))
        .setDelimiter(DELIMITER)
        .build());

      if (category.equals(COOKINGHOBS)) {
        printer.printRecord(
          product.getEprelCode(),
          product.getGtinCode(),
          product.getProductCode(),
          product.getCategory(),
          product.getCountryOfProduction(),
          product.getModel(),
          product.getBrand()
        );
      } else{
        printer.printRecord(
          product.getEprelCode(),
          product.getGtinCode(),
          product.getProductCode(),
          product.getCategory(),
          product.getCountryOfProduction()
        );
      }

      printer.flush();
      String csvString = out.toString();
      return CSVFormat.DEFAULT.parse(new StringReader(csvString)).getRecords().get(0);
    } catch (Exception e) {
      return null;
    }
  }
}


