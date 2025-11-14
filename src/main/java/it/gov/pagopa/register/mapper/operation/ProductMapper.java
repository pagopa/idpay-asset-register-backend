package it.gov.pagopa.register.mapper.operation;

import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.StatusChangeEvent;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

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

  private ProductMapper() {
  }

  private static final int MAX_NAME_LENGTH = 255;
  private static final int MAX_FIELD_100 = 100;
  private static final int MAX_GTIN_LENGTH = 14;

  public static ProductDTO toDTO(Product entity, String role) {
    if (entity == null) return null;

    List<StatusChangeEvent> chronology = getStatusChangeEvents(entity, role);

    return ProductDTO.builder()
      .organizationId(entity.getOrganizationId())
      .registrationDate(entity.getRegistrationDate().toString())
      .status(role.equals(UserRole.OPERATORE.getRole()) &&
        (entity.getStatus().equals(ProductStatus.WAIT_APPROVED.name()) || (entity.getStatus().equals(ProductStatus.SUPERVISED.name())))
        ? ProductStatus.UPLOADED.name()
        : entity.getStatus())
      .model(sanitizeBrandOrModelForDto(entity.getModel()))
      .productGroup(entity.getProductGroup())
      .category(CATEGORIES_TO_IT_S.get(entity.getCategory()))
      .brand(sanitizeBrandOrModelForDto(entity.getBrand()))
      .eprelCode(entity.getEprelCode())
      .gtinCode(sanitizeGtinForDto(entity.getGtinCode()))
      .productCode(sanitizeProductCodeForDto(entity.getProductCode()))
      .countryOfProduction(entity.getCountryOfProduction())
      .energyClass(entity.getEnergyClass())
      .linkEprel(generateEprelUrl(entity.getProductGroup(), entity.getEprelCode()))
      .batchName(CATEGORIES_TO_IT_P.get(entity.getCategory()) + "_" + entity.getProductFileId() + ".csv")
      .productName(limitName(entity.getProductName()))
      .fullProductName(limitName(entity.getFullProductName()))
      .capacity(entity.getCapacity() == null || "N\\A".equals(entity.getCapacity()) ? "" : entity.getCapacity())
      .statusChangeChronology(chronology)
      .formalMotivation(entity.getFormalMotivation())
      .organizationName(entity.getOrganizationName())
      .build();
  }

  private static List<StatusChangeEvent> getStatusChangeEvents(Product entity, String role) {
    List<StatusChangeEvent> chronology;

    if (entity.getStatusChangeChronology() == null) {
      chronology = new ArrayList<>();
    } else if (UserRole.OPERATORE.getRole().equals(role)) {
      chronology = entity.getStatusChangeChronology().stream()
        .map(e -> StatusChangeEvent.builder()
          .username("-")
          .role("-")
          .motivation("-")
          .updateDate(e.getUpdateDate())
          .currentStatus(e.getCurrentStatus())
          .targetStatus(e.getTargetStatus())
          .build())
        .toList();
    } else {
      chronology = entity.getStatusChangeChronology();
    }
    return chronology;
  }

  public static Product mapCookingHobToProduct(CSVRecord csvRecord, String orgId, String productFileId, String organizationName) {

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

  public static Product mapEprelToProduct(CSVRecord csvRecord, EprelProduct eprelData, String orgId, String productFileId, String category, String organizationName) {
    String capacity = mapCapacity(category, eprelData);

    String codeProduct = normalizeCsvCode(csvRecord.get(CODE_PRODUCT));
    String gtinCode = normalizeCsvCode(csvRecord.get(CODE_GTIN_EAN));
    String normalizedCategory = category != null ? category.trim().replaceAll("\\s+", "") : null;

    String productName = limitName(mapName(null, eprelData, normalizedCategory, capacity));
    String fullProductName = limitName(mapName(gtinCode, eprelData, normalizedCategory, capacity));

    return Product.builder()
      .productFileId(productFileId)
      .organizationId(orgId)
      .registrationDate(LocalDateTime.now(ZoneOffset.UTC))
      .status(ProductStatus.UPLOADED.name())
      .productCode(codeProduct)
      .gtinCode(gtinCode)
      .eprelCode(csvRecord.get(CODE_EPREL))
      .category(normalizedCategory)
      .productGroup(eprelData.getProductGroup())
      .countryOfProduction(csvRecord.get(COUNTRY_OF_PRODUCTION))
      .brand(eprelData.getSupplierOrTrademark())
      .model(eprelData.getModelIdentifier())
      .energyClass(mapEnergyClass(eprelData.getEnergyClass()))
      .capacity(capacity)
      .productName(productName)
      .fullProductName(fullProductName)
      .organizationName(organizationName)
      .statusChangeChronology(new ArrayList<>())
      .formalMotivation("")
      .build();
  }

  public static String mapCapacity(String category, EprelProduct eprelData) {
    if (eprelData == null) return "N\\A";
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
        } else yield "N\\A";
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

  private static String resolveProductType(EprelProduct eprel, String category) {
    if (REFRIGERATINGAPPL.equals(category)) {
      boolean isRefrigerator = eprel.getCompartments().stream()
        .anyMatch(c -> {
          if (REFRIGERATORS_CATEGORY.contains(c.getCompartmentType()))
            return true;
          if (VARIABLE_TEMP.equals(c.getCompartmentType())) {
            return c.getSubCompartments() != null &&
              c.getSubCompartments().stream()
                .map(EprelProduct.SubCompartment::getCompartmentType)
                .anyMatch(REFRIGERATORS_CATEGORY::contains);
          }
          return false;
        });
      return isRefrigerator ? REFRIGERATOR_IT : FREEZER_IT;
    } else {
      return CATEGORIES_TO_IT_S.get(category);
    }
  }

  public static String mapName(String gtinOrNull, EprelProduct eprel, String category, String capacity) {
    String type = resolveProductType(eprel, category);
    StringBuilder sb = new StringBuilder();
    if (gtinOrNull != null && !gtinOrNull.isBlank()) {
      sb.append(gtinOrNull).append(" - ");
    }
    sb.append(type).append(" ")
      .append(eprel.getSupplierOrTrademark()).append(" ")
      .append(eprel.getModelIdentifier());
    if (!"N\\A".equals(capacity)) {
      sb.append(" ").append(capacity);
    }
    return sb.toString();
  }

  private static String normalizeCsvCode(String value) {
    if (value == null) {
      return null;
    }
    return value.trim().replaceAll("\\s+", "");
  }

  private static String limitName(String value) {
    if (value == null) {
      return null;
    }

    String v = value.trim();

    v = v.replaceAll("\\s+", " ");

    if (v.length() > MAX_NAME_LENGTH) {
      v = v.substring(0, MAX_NAME_LENGTH);
    }

    return v;
  }

  private static String sanitizeBrandOrModelForDto(String value) {
    if (value == null) {
      return null;
    }

    String v = value.trim().replaceAll("\\s+", " ");

    if (v.length() > MAX_FIELD_100) {
      v = v.substring(0, MAX_FIELD_100);
    }

    return v;
  }

  private static String sanitizeProductCodeForDto(String value) {
    if (value == null) {
      return null;
    }

    String v = value.trim();

    v = v.replaceAll("[^a-zA-Z0-9 ]", "");

    v = v.replaceAll("\\s+", " ");

    if (v.length() > MAX_FIELD_100) {
      v = v.substring(0, MAX_FIELD_100);
    }

    return v;
  }

  private static String sanitizeGtinForDto(String value) {
    if (value == null) {
      return null;
    }

    String v = value.trim();

    v = v.replaceAll("\\s+", "");

    v = v.replaceAll("[^a-zA-Z0-9]", "");

    if (v.length() > MAX_GTIN_LENGTH) {
      v = v.substring(0, MAX_GTIN_LENGTH);
    }

    return v;
  }
}  
