package it.gov.pagopa.register.mapper.operation;

import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.enums.IpConnection;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.StatusChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.Category.Labels.CATEGORIES_FOR_DTO;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.Category.Labels.CATEGORIES_FOR_FILENAME;
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
      .category(CATEGORIES_FOR_DTO.get(entity.getCategory()))
      .categoryCode(entity.getCategory())
      .brand(sanitizeBrandOrModelForDto(entity.getBrand()))
      .eprelCode(entity.getEprelCode())
      .gtinCode(sanitizeGtinForDto(entity.getGtinCode()))
      .productCode(entity.getProductCode())
      .countryOfProduction(entity.getCountryOfProduction())
      .energyClass(entity.getEnergyClass())
      .linkEprel(generateEprelUrl(entity.getProductGroup(), entity.getEprelCode()))
      .batchName(CATEGORIES_FOR_FILENAME.get(entity.getCategory()) + "_" + entity.getProductFileId() + ".csv")
      .productName(entity.getProductName())
      .fullProductName(entity.getFullProductName())
      .capacity(entity.getCapacity() == null || "N\\A".equals(entity.getCapacity()) ? "" : entity.getCapacity())
      .statusChangeChronology(chronology)
      .formalMotivation(entity.getFormalMotivation())
      .organizationName(entity.getOrganizationName())
      .scart(entity.getScart())
      .connectivity(normalizeConnectivityForOutput(entity.getConnectivity()))
      .technicalSheetUrl(entity.getTechnicalSheetUrl())
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

    if (v.length() > MAX_GTIN_LENGTH) {
      v = v.substring(0, MAX_GTIN_LENGTH);
    }

    return v;
  }

  public static Boolean parseScart(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    String trimmed = value.trim();
    if ("SI".equalsIgnoreCase(trimmed) || "TRUE".equalsIgnoreCase(trimmed) || "1".equalsIgnoreCase(trimmed)) {
      return Boolean.TRUE;
    }
    if ("NO".equalsIgnoreCase(trimmed) || "FALSE".equalsIgnoreCase(trimmed) || "0".equalsIgnoreCase(trimmed)) {
      return Boolean.FALSE;
    }
    return false;
  }

  public static IpConnection normalizeConnectivity(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.replaceAll("[\\s_-]", "").toLowerCase(Locale.ROOT);

    return switch (normalized) {
      case "lan" -> IpConnection.LAN;
      case "wifi" -> IpConnection.WI_FI;
      case "lanewifi", "lanwifi" -> IpConnection.LAN_WI_FI;
      case "nessuna", "nessuno" -> IpConnection.NONE;
      default -> null;
    };
  }

  public static String normalizeConnectivityForOutput(String value) {
    IpConnection connection = normalizeConnectivity(value);
    return connection == null ? value : connection.getConnection();
  }

  public static String normalizeTechnicalSheetUrl(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim().replaceAll("\\s+", "");
    return normalized.regionMatches(true, 0, "www.", 0, 4)
      ? "https://" + normalized
      : normalized;
  }

}
