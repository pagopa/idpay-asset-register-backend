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

  @Test
  void dbCheck_returnsTrueWhenProductDoesNotExist() {
    CSVRecord record = mock(CSVRecord.class);
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    boolean result = ValidationUtils.dbCheck("org", record, Optional.empty(), invalidRecords, errorMessages, List.of());

    assertTrue(result);
    assertTrue(invalidRecords.isEmpty());
    assertTrue(errorMessages.isEmpty());
  }

  @Test
  void dbCheck_rejectsProductsFromDifferentOrganization() {
    CSVRecord record = mock(CSVRecord.class);
    Product product = Product.builder().organizationId("other").status("LOADED").build();
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    boolean result = ValidationUtils.dbCheck("org", record, Optional.of(product), invalidRecords, errorMessages, List.of("LOADED"));

    assertFalse(result);
    assertEquals(List.of(record), invalidRecords);
    assertEquals(DIFFERENT_ORGANIZATIONID, errorMessages.get(record));
  }

  @Test
  void dbCheck_rejectsProductsWithStatusNotAllowedForReload() {
    CSVRecord record = mock(CSVRecord.class);
    Product product = Product.builder().organizationId("org").status("APPROVED").build();
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    boolean result = ValidationUtils.dbCheck("org", record, Optional.of(product), invalidRecords, errorMessages, List.of("LOADED"));

    assertFalse(result);
    assertEquals(STATUS_NOT_VALID, errorMessages.get(record));
  }

  @Test
  void addError_appendsRecordAndMessage() {
    CSVRecord record = mock(CSVRecord.class);
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    ValidationUtils.addError(record, "message", invalidRecords, errorMessages);

    assertEquals(List.of(record), invalidRecords);
    assertEquals("message", errorMessages.get(record));
  }
}
