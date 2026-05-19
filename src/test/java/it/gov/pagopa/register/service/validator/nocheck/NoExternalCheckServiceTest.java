package it.gov.pagopa.register.service.validator.nocheck;

import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.mapper.product.ProductMapperStrategy;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.utils.ValidationUtils;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.DUPLICATE_GTIN_EAN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoExternalCheckServiceTest {

  private final ProductRepository productRepository = mock(ProductRepository.class);
  private final ProductMapperStrategy mapper = mock(ProductMapperStrategy.class);
  private final CategoryConfig categoryConfig = mock(CategoryConfig.class);

  private static final String CATEGORY = "CATEGORY";
  private static final String PRODUCT_MAPPER = "productMapper";
  private static final String ORG_ID = "orgId";
  private static final String INITIATIVE_ID = "initiativeId";
  private static final String PRODUCT_FILE_ID = "productFileId";
  private static final String ORGANIZATION_NAME = "organizationName";

  private final List<String> headers = List.of("header1", "header2");
  private final List<String> allowedReloadStatuses = List.of("REJECTED");

  @Test
  void validateRecords_shouldThrowException_whenMapperIsNotConfigured() {
    when(categoryConfig.getProductMapper()).thenReturn(PRODUCT_MAPPER);

    NoExternalCheckService service =
      new NoExternalCheckService(productRepository, Collections.emptyMap());

    CSVRecord csvRecord = mock(CSVRecord.class);

    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> service.validateRecords(
        List.of(csvRecord),
        CATEGORY,
        ORG_ID,
        INITIATIVE_ID,
        PRODUCT_FILE_ID,
        headers,
        ORGANIZATION_NAME,
        categoryConfig,
        allowedReloadStatuses
      )
    );

    assertEquals(
      "No ProductMapperStrategy configured for category: " + CATEGORY,
      exception.getMessage()
    );
  }

  @Test
  void validateRecords_shouldReturnValidProduct_whenRecordIsValidAndProductDoesNotExistOnDb() {
    CSVRecord record = mock(CSVRecord.class);
    Product product = mock(Product.class);

    NoExternalCheckService service = buildService();

    when(categoryConfig.getProductMapper()).thenReturn(PRODUCT_MAPPER);
    when(mapper.extractBusinessKey(record, categoryConfig)).thenReturn("EAN_1");
    when(productRepository.findByGtinCodeAndInitiativeId("EAN_1", INITIATIVE_ID))
      .thenReturn(Optional.empty());

    when(mapper.mapToProduct(
      record,
      CATEGORY,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      null
    )).thenReturn(product);

    try (MockedStatic<ValidationUtils> validationUtils = mockStatic(ValidationUtils.class)) {
      validationUtils.when(() -> ValidationUtils.dbCheck(
        eq(ORG_ID),
        eq(record),
        eq(Optional.empty()),
        anyList(),
        anyMap(),
        eq(allowedReloadStatuses)
      )).thenReturn(true);

      ProductValidationResult result = service.validateRecords(
        List.of(record),
        CATEGORY,
        ORG_ID,
        INITIATIVE_ID,
        PRODUCT_FILE_ID,
        headers,
        ORGANIZATION_NAME,
        categoryConfig,
        allowedReloadStatuses
      );

      assertEquals(1, result.getValidRecords().size());
      assertSame(product, result.getValidRecords().get("EAN_1"));
      assertTrue(result.getInvalidRecords().isEmpty());
      assertTrue(result.getErrorMessages().isEmpty());

      verify(mapper).mapToProduct(
        record,
        CATEGORY,
        ORG_ID,
        INITIATIVE_ID,
        PRODUCT_FILE_ID,
        ORGANIZATION_NAME,
        null
      );
    }
  }

  @Test
  void validateRecords_shouldCopyDbFields_whenRecordIsValidAndProductAlreadyExistsOnDb() {
    CSVRecord csvRecord = mock(CSVRecord.class);

    Product dbProduct = mock(Product.class);
    Product mappedProduct = mock(Product.class);

    NoExternalCheckService service = buildService();

    when(categoryConfig.getProductMapper()).thenReturn(PRODUCT_MAPPER);
    when(mapper.extractBusinessKey(csvRecord, categoryConfig)).thenReturn("EAN_1");

    Optional<Product> existing = Optional.of(dbProduct);

    when(productRepository.findByGtinCodeAndInitiativeId("EAN_1", INITIATIVE_ID))
      .thenReturn(existing);

    when(dbProduct.getFormalMotivation()).thenReturn("formal motivation");
    when(dbProduct.getStatusChangeChronology()).thenReturn(null);

    when(mapper.mapToProduct(
      csvRecord,
      CATEGORY,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      null
    )).thenReturn(mappedProduct);

    try (MockedStatic<ValidationUtils> validationUtils = mockStatic(ValidationUtils.class)) {
      validationUtils.when(() -> ValidationUtils.dbCheck(
        eq(ORG_ID),
        eq(csvRecord),
        eq(existing),
        anyList(),
        anyMap(),
        eq(allowedReloadStatuses)
      )).thenReturn(true);

      ProductValidationResult result = service.validateRecords(
        List.of(csvRecord),
        CATEGORY,
        ORG_ID,
        INITIATIVE_ID,
        PRODUCT_FILE_ID,
        headers,
        ORGANIZATION_NAME,
        categoryConfig,
        allowedReloadStatuses
      );

      assertEquals(1, result.getValidRecords().size());
      assertSame(mappedProduct, result.getValidRecords().get("EAN_1"));

      verify(mappedProduct).setFormalMotivation("formal motivation");
      verify(mappedProduct).setStatusChangeChronology(null);
    }
  }

  @Test
  void validateRecords_shouldReturnInvalidRecord_whenDbCheckFails() {
    CSVRecord csvRecord = mock(CSVRecord.class);

    NoExternalCheckService service = buildService();

    when(categoryConfig.getProductMapper()).thenReturn(PRODUCT_MAPPER);
    when(mapper.extractBusinessKey(csvRecord, categoryConfig)).thenReturn("EAN_1");

    Optional<Product> existing = Optional.empty();

    when(productRepository.findByGtinCodeAndInitiativeId("EAN_1", INITIATIVE_ID))
      .thenReturn(existing);

    try (MockedStatic<ValidationUtils> validationUtils = mockStatic(ValidationUtils.class)) {
      validationUtils.when(() -> ValidationUtils.dbCheck(
        eq(ORG_ID),
        eq(csvRecord),
        eq(existing),
        anyList(),
        anyMap(),
        eq(allowedReloadStatuses)
      )).thenAnswer(invocation -> {
        List<CSVRecord> invalidRecords = invocation.getArgument(3);
        Map<CSVRecord, String> errorMessages = invocation.getArgument(4);

        invalidRecords.add(csvRecord);
        errorMessages.put(csvRecord, "DB_ERROR");

        return false;
      });

      ProductValidationResult result = service.validateRecords(
        List.of(csvRecord),
        CATEGORY,
        ORG_ID,
        INITIATIVE_ID,
        PRODUCT_FILE_ID,
        headers,
        ORGANIZATION_NAME,
        categoryConfig,
        allowedReloadStatuses
      );

      assertTrue(result.getValidRecords().isEmpty());
      assertEquals(List.of(csvRecord), result.getInvalidRecords());
      assertEquals("DB_ERROR", result.getErrorMessages().get(csvRecord));

      verify(mapper, never()).mapToProduct(
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any()
      );
    }
  }

  @Test
  void validateRecords_shouldRemoveFirstValidProductAndMarkItInvalid_whenDuplicateBusinessKeyIsFound() {
    CSVRecord firstRecord = mock(CSVRecord.class);
    CSVRecord secondRecord = mock(CSVRecord.class);
    CSVRecord duplicateCsvRow = mock(CSVRecord.class);

    Product firstProduct = mock(Product.class);

    NoExternalCheckService service = buildService();

    when(categoryConfig.getProductMapper()).thenReturn(PRODUCT_MAPPER);

    when(mapper.extractBusinessKey(firstRecord, categoryConfig)).thenReturn("EAN_DUPLICATE");
    when(mapper.extractBusinessKey(secondRecord, categoryConfig)).thenReturn("EAN_DUPLICATE");

    when(productRepository.findByGtinCodeAndInitiativeId("EAN_DUPLICATE", INITIATIVE_ID))
      .thenReturn(Optional.empty());

    when(mapper.mapToProduct(
      firstRecord,
      CATEGORY,
      ORG_ID,
      INITIATIVE_ID,
      PRODUCT_FILE_ID,
      ORGANIZATION_NAME,
      null
    )).thenReturn(firstProduct);

    when(mapper.mapToCsvRow(firstProduct, headers)).thenReturn(duplicateCsvRow);

    try (MockedStatic<ValidationUtils> validationUtils = mockStatic(ValidationUtils.class)) {
      validationUtils.when(() -> ValidationUtils.dbCheck(
        eq(ORG_ID),
        any(CSVRecord.class),
        eq(Optional.empty()),
        anyList(),
        anyMap(),
        eq(allowedReloadStatuses)
      )).thenReturn(true);

      ProductValidationResult result = service.validateRecords(
        List.of(firstRecord, secondRecord),
        CATEGORY,
        ORG_ID,
        INITIATIVE_ID,
        PRODUCT_FILE_ID,
        headers,
        ORGANIZATION_NAME,
        categoryConfig,
        allowedReloadStatuses
      );

      assertTrue(result.getValidRecords().isEmpty());

      assertEquals(1, result.getInvalidRecords().size());
      assertSame(duplicateCsvRow, result.getInvalidRecords().getFirst());

      assertEquals(
        DUPLICATE_GTIN_EAN,
        result.getErrorMessages().get(duplicateCsvRow)
      );

      verify(mapper).mapToCsvRow(firstProduct, headers);

      verify(mapper, never()).mapToProduct(
        eq(secondRecord),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any()
      );
    }
  }

  private NoExternalCheckService buildService() {
    return new NoExternalCheckService(
      productRepository,
      Map.of(PRODUCT_MAPPER, mapper)
    );
  }
}
