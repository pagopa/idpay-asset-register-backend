package it.gov.pagopa.register.utils;

import it.gov.pagopa.register.model.operation.Product;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.DIFFERENT_ORGANIZATIONID;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.STATUS_NOT_VALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ValidationUtilsTest {

  public static final String DM_DATE = "01/01/2026";
  public static final String ORG = "org";

  @Test
  void dbCheck_returnsTrueWhenProductDoesNotExist() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    boolean result = ValidationUtils.dbCheck(ORG, csvRecord, Optional.empty(), invalidRecords, errorMessages, List.of(), DM_DATE);

    assertTrue(result);
    assertTrue(invalidRecords.isEmpty());
    assertTrue(errorMessages.isEmpty());
  }

  @Test
  void dbCheck_rejectsProductsFromDifferentOrganization() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    Product product = Product.builder().organizationId("other").status("LOADED").build();
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    boolean result = ValidationUtils.dbCheck(ORG, csvRecord, Optional.of(product), invalidRecords, errorMessages, List.of("LOADED"), DM_DATE);

    assertFalse(result);
    assertEquals(List.of(csvRecord), invalidRecords);
    assertEquals(DIFFERENT_ORGANIZATIONID, errorMessages.get(csvRecord));
  }

  @Test
  void dbCheck_rejectsProductsWithStatusNotAllowedForReload() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    Product product = Product.builder().organizationId(ORG).status("APPROVED").build();
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    boolean result = ValidationUtils.dbCheck(ORG, csvRecord, Optional.of(product), invalidRecords, errorMessages, List.of("LOADED"), DM_DATE);

    assertFalse(result);
    assertEquals(STATUS_NOT_VALID, errorMessages.get(csvRecord));
  }

  @Test
  void addError_appendsRecordAndMessage() {
    CSVRecord csvRecord = mock(CSVRecord.class);
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    ValidationUtils.addError(csvRecord, "message", invalidRecords, errorMessages);

    assertEquals(List.of(csvRecord), invalidRecords);
    assertEquals("message", errorMessages.get(csvRecord));
  }
}
