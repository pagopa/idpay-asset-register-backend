package it.gov.pagopa.register.constants;

import org.junit.jupiter.api.Test;

import static it.gov.pagopa.register.constants.ValidationPatterns.BRAND;
import static it.gov.pagopa.register.constants.ValidationPatterns.GTIN_CODE;
import static it.gov.pagopa.register.constants.ValidationPatterns.MODEL;
import static it.gov.pagopa.register.constants.ValidationPatterns.PRODUCT_CODE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationPatternsTest {

  @Test
  void shouldAcceptInternationalCharactersAndSymbolsInDescriptiveFields() {
    assertTrue("Caffè & tè S.p.A.".matches(BRAND));
    assertTrue("Modèle Série-2 (50\")".matches(MODEL));
  }

  @Test
  void shouldAcceptOnlyAsciiAlphanumericGtin() {
    assertTrue("ABC123".matches(GTIN_CODE));
    assertFalse("ABC123é".matches(GTIN_CODE));
    assertFalse("ABC-123".matches(GTIN_CODE));
  }

  @Test
  void shouldAcceptUsefulAsciiProductCodeCharacters() {
    assertTrue("PRD/2026_A+B#1 (rev.2)".matches(PRODUCT_CODE));
  }

  @Test
  void shouldRejectForbiddenProductCodeCharactersAndAccents() {
    for (String forbidden : new String[]{"&", "\"", "'", "<", ">", "%", "$", ":", ",", ";", "="}) {
      assertFalse(("ABC" + forbidden + "123").matches(PRODUCT_CODE));
    }
    assertFalse("PROD-é".matches(PRODUCT_CODE));
  }

  @Test
  void shouldRejectControlCharactersAndInvalidLengths() {
    assertFalse("Brand\nInjected row".matches(BRAND));
    assertFalse("Model\tInjected column".matches(MODEL));
    assertFalse("".matches(PRODUCT_CODE));
    assertFalse("A".repeat(101).matches(PRODUCT_CODE));
    assertFalse("A".repeat(15).matches(GTIN_CODE));
  }
}
