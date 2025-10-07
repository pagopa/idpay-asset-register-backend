package it.gov.pagopa.register.mapper.operation;

import it.gov.pagopa.register.dto.operation.FormalMotivationDTO;
import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.model.operation.Product;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.time.Instant;
import java.time.OffsetDateTime;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.utils.CsvUtils.DELIMITER;
import static it.gov.pagopa.register.utils.EprelUtils.generateEprelUrl;
import static it.gov.pagopa.register.utils.EprelUtils.mapEnergyClass;


public class ProductMapper {



  private ProductMapper() {}

  public static ProductDTO toDTO(Product entity, String role){

    if(entity==null){
      return null;
    }

    FormalMotivationDTO fm = entity.getFormalMotivation() != null
      ? entity.getFormalMotivation()
      : new FormalMotivationDTO("-", LocalDateTime.of(1970, 01, 01, 00, 00));

    return ProductDTO.builder()
      .organizationId(entity.getOrganizationId())
      .registrationDate(entity.getRegistrationDate())
      .status(role.equals(UserRole.OPERATORE.getRole()) && entity.getStatus().equals(ProductStatus.WAIT_APPROVED.name()) ? ProductStatus.UPLOADED.name() : entity.getStatus())
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
      .capacity(entity.getCapacity() == null || "N\\A".equals(entity.getCapacity()) ? "" : entity.getCapacity())
      .statusChangeChronology(entity.getStatusChangeChronology() == null || role.equals(UserRole.OPERATORE.getRole()) ? new ArrayList<>() : entity.getStatusChangeChronology())
      .formalMotivation(fm)
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
      .statusChangeChronology(new ArrayList<>())
      .formalMotivation(new FormalMotivationDTO("-", LocalDateTime.of(1970, 01, 01, 00, 00)))
      .build();
  }

  public static Product mapEprelToProduct(CSVRecord csvRecord, EprelProduct eprelData, String orgId, String productFileId, String category, String organizationName) {
    String capacity = mapCapacity(category,eprelData);

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
      .productName(mapProductName(eprelData, category, capacity))
      .organizationName(organizationName)
      .statusChangeChronology(new ArrayList<>())
      .formalMotivation(new FormalMotivationDTO("-", LocalDateTime.of(1970, 01, 01, 00, 00)))
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

  public static String mapProductName(EprelProduct eprel, String category, String capacity) {
    String productType;

    if (category.equals(REFRIGERATINGAPPL)) {
      productType = eprel.getCompartments().stream()
        .filter(c -> {
          if (REFRIGERATORS_CATEGORY.contains(c.getCompartmentType())) {
            return true;
          }
          if (VARIABLE_TEMP.equals(c.getCompartmentType())) {
            return c.getSubCompartments() != null &&
              c.getSubCompartments().stream()
                .map(EprelProduct.SubCompartment::getCompartmentType)
                .anyMatch(REFRIGERATORS_CATEGORY::contains);
          }
          return false;
        })
        .findFirst()
        .map(c -> REFRIGERATOR_IT)
        .orElse(FREEZER_IT);
    } else {
      productType = CATEGORIES_TO_IT_S.get(category);
    }

    return buildProductName(productType, eprel, capacity);
  }

  private static String buildProductName(String type, EprelProduct eprel, String capacity) {
    StringBuilder name = new StringBuilder();
    name.append(type).append(" ")
      .append(eprel.getSupplierOrTrademark()).append(" ")
      .append(eprel.getModelIdentifier());

    if (!"N\\A".equals(capacity)) {
      name.append(" ").append(capacity);
    }

    return name.toString();
  }

}
