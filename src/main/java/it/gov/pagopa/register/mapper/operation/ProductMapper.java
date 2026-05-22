package it.gov.pagopa.register.mapper.operation;

import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.StatusChangeEvent;

import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.utils.EprelUtils.generateEprelUrl;

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
      .initiativeId(entity.getInitiativeId())
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
      .batchName(CATEGORIES_FOR_FILENAME.get(entity.getCategory()) + "_" + entity.getProductFileId() + ".csv")
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

  public static String normalizeCsvCode(String value) {
    if (value == null) {
      return null;
    }
    return value.trim().replaceAll("\\s+", "");
  }

  public static String limitName(String value) {
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

  public static String sanitizeBrandOrModelForDto(String value) {
    if (value == null) {
      return null;
    }

    String v = value.trim().replaceAll("\\s+", " ");

    if (v.length() > MAX_FIELD_100) {
      v = v.substring(0, MAX_FIELD_100);
    }

    return v;
  }

  public static String sanitizeProductCodeForDto(String value) {
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

  public static String sanitizeGtinForDto(String value) {
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

  private static String resolveProductType(EprelProduct eprel, String category) {
    if (REFRIGERATINGAPPL.equals(category)) {
      boolean isRefrigerator =
        eprel.getCompartments().stream().anyMatch(c -> {
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
        });

      return isRefrigerator ? REFRIGERATOR_IT : FREEZER_IT;
    }

    return CATEGORIES_TO_IT_S.get(category);
  }
  public static String mapName(
    String gtinOrNull,
    EprelProduct eprel,
    String category,
    String capacity
  ) {
    String type = resolveProductType(eprel, category);

    StringBuilder sb = new StringBuilder();
    if (gtinOrNull != null && !gtinOrNull.isBlank()) {
      sb.append(gtinOrNull).append(" - ");
    }

    sb.append(type)
      .append(" ")
      .append(eprel.getSupplierOrTrademark())
      .append(" ")
      .append(eprel.getModelIdentifier());

    if (!"N\\A".equals(capacity)) {
      sb.append(" ").append(capacity);
    }

    return sb.toString();
  }
}
