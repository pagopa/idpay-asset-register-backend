package it.gov.pagopa.register.utils;

import it.gov.pagopa.register.enums.IpConnection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductFieldSanitizerTest {

  @Test
  void testParseScart() {
    assertTrue(ProductFieldSanitizer.parseScart("SI"));
    assertTrue(ProductFieldSanitizer.parseScart("si"));
    assertTrue(ProductFieldSanitizer.parseScart("  SI  "));
    assertTrue(ProductFieldSanitizer.parseScart("SÌ"));
    assertTrue(ProductFieldSanitizer.parseScart("sì"));
    assertTrue(ProductFieldSanitizer.parseScart("  Sì  "));
    assertTrue(ProductFieldSanitizer.parseScart("TRUE"));
    assertTrue(ProductFieldSanitizer.parseScart("True"));
    assertTrue(ProductFieldSanitizer.parseScart("1"));
    assertFalse(ProductFieldSanitizer.parseScart("NO"));
    assertFalse(ProductFieldSanitizer.parseScart("no"));
    assertFalse(ProductFieldSanitizer.parseScart("FALSE"));
    assertFalse(ProductFieldSanitizer.parseScart("False"));
    assertFalse(ProductFieldSanitizer.parseScart("0"));
    assertFalse(ProductFieldSanitizer.parseScart("INVALID"));
    assertFalse(ProductFieldSanitizer.parseScart(""));
    assertFalse(ProductFieldSanitizer.parseScart("   "));
    assertFalse(ProductFieldSanitizer.parseScart(null));
  }

  @Test
  void testNormalizeConnectivity() {
    assertEquals(IpConnection.LAN, ProductFieldSanitizer.normalizeConnectivity("LAN"));
    assertEquals(IpConnection.WI_FI, ProductFieldSanitizer.normalizeConnectivity("wifi"));
    assertEquals(IpConnection.LAN_WI_FI, ProductFieldSanitizer.normalizeConnectivity("  LAN   e   WIFI  "));
    assertEquals(IpConnection.NONE, ProductFieldSanitizer.normalizeConnectivity("Nessuna"));
    assertNull(ProductFieldSanitizer.normalizeConnectivity(null));
    assertNull(ProductFieldSanitizer.normalizeConnectivity(""));
    assertNull(ProductFieldSanitizer.normalizeConnectivity("   "));
    assertNull(ProductFieldSanitizer.normalizeConnectivity("Bluetooth"));
  }

  @Test
  void testNormalizeTechnicalSheetUrl() {
    assertEquals("https://example.com/sheet", ProductFieldSanitizer.normalizeTechnicalSheetUrl("  https://example.com/  sheet  "));
    assertEquals("https://www.google.com", ProductFieldSanitizer.normalizeTechnicalSheetUrl(" www.google.com "));

    String longUrl = "https://example.com/" + "a".repeat(300);
    assertEquals(longUrl.length(), ProductFieldSanitizer.normalizeTechnicalSheetUrl(longUrl).length());

    assertNull(ProductFieldSanitizer.normalizeTechnicalSheetUrl(null));
    assertNull(ProductFieldSanitizer.normalizeTechnicalSheetUrl("   "));

    assertEquals("Nessuna", ProductFieldSanitizer.normalizeTechnicalSheetUrl("Nessuna"));
    assertEquals("Nessuna", ProductFieldSanitizer.normalizeTechnicalSheetUrl("nessuna"));
    assertEquals("Nessuna", ProductFieldSanitizer.normalizeTechnicalSheetUrl(" NESSUNA "));
  }

}
