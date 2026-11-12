package it.gov.pagopa.register.utils;

import it.gov.pagopa.register.enums.IpConnection;

import java.util.Locale;

public class ProductFieldSanitizer {

  private ProductFieldSanitizer() {
  }

  private static final int MAX_NAME_LENGTH = 255;
  private static final int MAX_FIELD_100 = 100;
  private static final int MAX_GTIN_LENGTH = 14;

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
    if ("SI".equalsIgnoreCase(trimmed) || "SÌ".equalsIgnoreCase(trimmed)
      || "TRUE".equalsIgnoreCase(trimmed) || "1".equalsIgnoreCase(trimmed)) {
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

  public static String normalizeTechnicalSheetUrl(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    if ("nessuna".equalsIgnoreCase(normalized)) {
      return "Nessuna";
    }
    return normalized.regionMatches(true, 0, "www.", 0, 4)
      ? "https://" + normalized
      : normalized;
  }

}
