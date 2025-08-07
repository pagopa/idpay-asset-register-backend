package it.gov.pagopa.register.mapper.operation;

import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

import java.util.stream.Collectors;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.utils.CsvUtils.DELIMITER;
import static it.gov.pagopa.register.utils.EprelUtils.*;


public class ProductMapper {



  private ProductMapper() {}

  public static ProductDTO toDTO(Product entity){

    if(entity==null){
      return null;
    }

    return ProductDTO.builder()
      .organizationId(entity.getOrganizationId())
      .registrationDate(entity.getRegistrationDate())
      .status(entity.getStatus())
      .model(entity.getModel())
      .productGroup(entity.getProductGroup())
      .category(CATEGORIES_TO_IT_S.get(entity.getCategory()))
      .brand(entity.getBrand())
      .eprelCode(entity.getEprelCode())
      .gtinCode(entity.getGtinCode())
      .productCode(entity.getProductCode())
      .countryOfProduction(entity.getCountryOfProduction())
      .energyClass(entity.getEnergyClass())
      .linkEprel(generateEprelUrl(entity.getProductGroup(), entity.getEprelCode()))
      .batchName(CATEGORIES_TO_IT_P.get(entity.getCategory())+"_"+entity.getProductFileId()+".csv")
      .productName(entity.getProductName())
      .capacity(("N\\A").equals(entity.getCapacity()) ? null : entity.getCapacity())
      .motivation(entity.getMotivation())
      .organizationName(entity.getOrganizationName())
      .build();
  }
  public static Product mapCookingHobToProduct(CSVRecord csvRecord, String orgId, String productFileId,String organizationName) {
    return Product.builder()
      .productFileId(productFileId)
      .organizationId(orgId)
      .registrationDate(LocalDateTime.now())
      .status(ProductStatus.UPLOADED.name())
      .productCode(csvRecord.get(CODE_PRODUCT))
      .gtinCode(csvRecord.get(CODE_GTIN_EAN))
      .category(COOKINGHOBS)
      .countryOfProduction(csvRecord.get(COUNTRY_OF_PRODUCTION))
      .brand(csvRecord.get(BRAND))
      .model(csvRecord.get(MODEL))
      .capacity("N\\A")
      .productName(CATEGORIES_TO_IT_S.get(COOKINGHOBS) +" "+
        csvRecord.get(BRAND)+" "+
        csvRecord.get(MODEL)
      )
      .organizationName(organizationName)
      .motivation(MOTIVATION_UPLOADED)
      .build();
  }

  public static Product mapEprelToProduct(CSVRecord csvRecord, EprelProduct eprelData, String orgId, String productFileId, String category, String organizationName) {
    String capacity = mapCapacity(category,eprelData);
    String productName = CATEGORIES_TO_IT_S.get(category) + " " +
      eprelData.getSupplierOrTrademark() + " " +
      eprelData.getModelIdentifier() +
      (!"N\\A".equals(capacity) ? " " + capacity : "");
    return Product.builder()
      .productFileId(productFileId)
      .organizationId(orgId)
      .registrationDate(LocalDateTime.now())
      .status(ProductStatus.UPLOADED.name())
      .productCode(csvRecord.get(CODE_PRODUCT))
      .gtinCode(csvRecord.get(CODE_GTIN_EAN))
      .eprelCode(csvRecord.get(CODE_EPREL))
      .category(category)
      .productGroup(eprelData.getProductGroup())
      .countryOfProduction(csvRecord.get(COUNTRY_OF_PRODUCTION))
      .brand(eprelData.getSupplierOrTrademark())
      .model(eprelData.getModelIdentifier())
      .energyClass(mapEnergyClass(eprelData.getEnergyClass()))
      .capacity(capacity)
      .productName(productName)
      .organizationName(organizationName)
      .motivation(MOTIVATION_UPLOADED)
      .build();
  }

  public static String mapCapacity(String category, EprelProduct eprelData) {
    if (eprelData == null) {
      return "N\\A";
    }

    return switch (category) {
      case WASHINGMACHINES, TUMBLEDRYERS ->
        eprelData.getRatedCapacity() != null ? eprelData.getRatedCapacity() + " kg" : "N\\A";
      case WASHERDRIERS ->
        eprelData.getRatedCapacityWash() != null ? eprelData.getRatedCapacityWash() + " kg" : "N\\A";
      case OVENS -> {
        if (eprelData.getCavities() != null && !eprelData.getCavities().isEmpty()) {
          yield eprelData.getCavities().stream()
            .map(cavity -> cavity.getVolume() != null ? cavity.getVolume() + " l" : "N\\A")
            .collect(Collectors.joining(" / "));
        } else {
          yield "N\\A";
        }
      }
      case DISHWASHERS ->
        eprelData.getRatedCapacity() != null ? eprelData.getRatedCapacity() + " c" : "N\\A";
      case REFRIGERATINGAPPL ->
        eprelData.getTotalVolume() != null ? eprelData.getTotalVolume() + " l" : "N\\A";
      default -> "N\\A";
    };
  }

  public static CSVRecord mapProductToCsvRow(Product product, String category, List<String> headers) {
    try {
      StringWriter out = new StringWriter();
      CSVPrinter printer = new CSVPrinter(out, CSVFormat.Builder.create()
        .setHeader(headers.toArray(new String[0]))
        .setDelimiter(DELIMITER)
        .build());

      if (COOKINGHOBS.equals(category)) {
        printer.printRecord(
          product.getEprelCode(),
          product.getGtinCode(),
          product.getProductCode(),
          product.getCategory(),
          product.getCountryOfProduction(),
          product.getModel(),
          product.getBrand()
        );
      } else {
        printer.printRecord(
          product.getEprelCode(),
          product.getGtinCode(),
          product.getProductCode(),
          product.getCategory(),
          product.getCountryOfProduction()
        );
      }
      CSVFormat format = CSVFormat.Builder.create()
        .setHeader(headers.toArray(new String[0]))
        .setSkipHeaderRecord(true)
        .setDelimiter(DELIMITER)
        .setTrim(true)
        .build();
      List<CSVRecord> records = format.parse(new StringReader(out.toString())).getRecords();
      return records.isEmpty() ? null : records.getFirst();
    } catch (Exception e) {
      return null;
    }
  }
}
