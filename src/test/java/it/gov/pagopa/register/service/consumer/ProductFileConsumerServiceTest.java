package it.gov.pagopa.register.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.register.connector.notification.NotificationServiceImpl;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.dto.operation.StorageEventDTO;
import it.gov.pagopa.register.dto.operation.StorageEventDTO.StorageEventData;
import it.gov.pagopa.register.dto.utils.EprelResult;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.service.validator.EprelProductValidatorService;
import it.gov.pagopa.register.utils.CsvUtils;
import it.gov.pagopa.register.dto.utils.EventDetails;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.messaging.Message;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProductFileConsumerServiceTest {

  @InjectMocks
  private ProductFileConsumerService service;

  @Mock
  private ProductRepository productRepository;
  @Mock
  private FileStorageClient fileStorageClient;
  @Mock
  private ProductFileRepository productFileRepository;
  @Mock
  private EprelProductValidatorService eprelProductValidator;
  @Mock
  private ObjectMapper objectMapper;
  @Mock
  private NotificationServiceImpl notificationService;

  private static final String ORG_ID = "ORG123";
  private static final String PRODUCT_FILE_ID = "file123";
  public static final Pattern SUBJECT_PATTERN =
    Pattern.compile(".*/blobs/CSV/([^/]+)/([^/]+)/([^/]+\\.csv)$");

  @BeforeEach
  void setUp() {
    when(objectMapper.readerFor(any(TypeReference.class)))
      .thenReturn(mock(ObjectReader.class));

    service = new ProductFileConsumerService(
      "test-app",
      productRepository,
      fileStorageClient,
      objectMapper,
      productFileRepository,
      eprelProductValidator,
      notificationService
    );
  }

  // Test: evento valido deve attivare il flusso completo di elaborazione
  @Test
  void testExecute_validEvent_shouldProcessFile() {
    StorageEventData data = StorageEventData.builder()
      .url("/CSV/ORG123/COOKINGHOBS/file123.csv")
      .build();

    StorageEventDTO event = StorageEventDTO.builder()
      .subject("/blobs/CSV/ORG123/COOKINGHOBS/file123.csv")
      .data(data)
      .build();

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    when(fileStorageClient.download(anyString())).thenReturn(stream);
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));
    when(productRepository.saveAll(any())).thenReturn(List.of());



    CSVRecord wrongOrgIdCsv = mock(CSVRecord.class);
    when(wrongOrgIdCsv.get(CODE_GTIN_EAN)).thenReturn("wrong-org-id-csv");

    CSVRecord wrongStatusCsv = mock(CSVRecord.class);
    when(wrongStatusCsv.get(CODE_GTIN_EAN)).thenReturn("wrong-status-csv");

    CSVRecord validProductCsv = mock(CSVRecord.class);
    when(validProductCsv.get(CODE_GTIN_EAN)).thenReturn("valid-gtin");
    when(validProductCsv.get(BRAND)).thenReturn("valid-gtin");
    when(validProductCsv.get(MODEL)).thenReturn("valid-gtin");
    when(validProductCsv.get(COUNTRY_OF_PRODUCTION)).thenReturn("valid-gtin");
    when(validProductCsv.get(CODE_PRODUCT)).thenReturn("valid-gtin");

    CSVRecord duplicatedProductCsv = mock(CSVRecord.class);
    when(duplicatedProductCsv.get(CODE_GTIN_EAN)).thenReturn("valid-gtin");
    when(duplicatedProductCsv.get(BRAND)).thenReturn("valid-gtin");
    when(duplicatedProductCsv.get(MODEL)).thenReturn("valid-gtin");
    when(duplicatedProductCsv.get(COUNTRY_OF_PRODUCTION)).thenReturn("valid-gtin");
    when(duplicatedProductCsv.get(CODE_PRODUCT)).thenReturn("valid-gtin");

    Product productWrongId = Product.builder()
      .organizationId("test")
      .status("APPROVED")
      .build();

    Product productWrontStatus = Product.builder()
      .organizationId("ORG123")
      .status("REJECTED")
      .build();

    when(productRepository.findById("wrong-org-id-csv")).thenReturn(Optional.of(productWrongId));
    when(productRepository.findById("wrong-status-csv")).thenReturn(Optional.of(productWrontStatus));
    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      utils.when(() -> CsvUtils.readHeader(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of(CODE_EPREL,CODE_GTIN_EAN));
      utils.when(() -> CsvUtils.readCsvRecords(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of(wrongOrgIdCsv,wrongStatusCsv,validProductCsv,duplicatedProductCsv));
      assertDoesNotThrow(() -> service.execute(List.of(event), null));
    }
  }

  @Test
  void testExecute_validEvent_shouldProcessFile_Eprel() {
    StorageEventData data = StorageEventData.builder()
      .url("/CSV/ORG123/WASHINGMACHINES/file123.csv")
      .build();
    StorageEventDTO event = StorageEventDTO.builder()
      .subject("/blobs/CSV/ORG123/WASHINGMACHINES/file123.csv")
      .data(data)
      .build();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    when(fileStorageClient.download(anyString())).thenReturn(stream);
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));
    when(productRepository.saveAll(any())).thenReturn(List.of());
    Map<String, Product> validRecords = new HashMap<>();
    validRecords.put("model123", new Product());

    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    when(eprelProductValidator.validateRecords(any(), any(), any(), any(), any(), any()))
      .thenReturn(new EprelResult(validRecords, invalidRecords, errorMessages));

    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      CSVRecord csvRecord = mock(CSVRecord.class);
      utils.when(() -> CsvUtils.readHeader(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of("HEADER"));
      utils.when(() -> CsvUtils.readCsvRecords(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of(csvRecord));
      assertDoesNotThrow(() -> service.execute(List.of(event), null));
    }}

  @Test
  void testExecute_validEvent_shouldProcessFile_Eprel_InvalidRecords() {
    StorageEventData data = StorageEventData.builder()
      .url("/CSV/ORG123/WASHINGMACHINES/file123.csv")
      .build();
    StorageEventDTO event = StorageEventDTO.builder()
      .subject("/blobs/CSV/ORG123/WASHINGMACHINES/file123.csv")
      .data(data)
      .build();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    when(fileStorageClient.download(anyString())).thenReturn(stream);
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));
    when(productRepository.saveAll(any())).thenReturn(List.of());
    Map<String, Product> validRecords = new HashMap<>();
    validRecords.put("model123", new Product());

    CSVRecord record1 = mock(CSVRecord.class);
    CSVRecord record2 = mock(CSVRecord.class);

    List<CSVRecord> invalidRecords = List.of(record1, record2);
    Map<CSVRecord, String> errorMessages = new HashMap<>();

    when(eprelProductValidator.validateRecords(any(), any(), any(), any(), any(), any()))
      .thenReturn(new EprelResult(validRecords, invalidRecords, errorMessages));

    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      CSVRecord csvRecord = mock(CSVRecord.class);
      utils.when(() -> CsvUtils.readHeader(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of("HEADER"));
      utils.when(() -> CsvUtils.readCsvRecords(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of(csvRecord));
      assertDoesNotThrow(() -> service.execute(List.of(event), null));
    }}


  // Test: URL non contenente /CSV/ deve restituire null
  @Test
  void testExtractBlobPath_invalidUrl_shouldReturnNull() {
    String url = "/wrongprefix/file.csv";
    assertNull(service.extractBlobPath(url));
  }

  // Test: file non trovato nel repository non deve generare errori
  @Test
  void testSetProductFileStatus_fileNotFound_shouldDoNothing() {
    when(productFileRepository.findById("missing-file")).thenReturn(Optional.empty());
    service.setProductFileStatus("missing-file", "PARTIAL", 0);
    verify(productFileRepository, never()).save(any());
  }

  // Test: subject valido viene correttamente parsato in EventDetails
  @Test
  void testParseEventSubject_validSubject_shouldReturnDetails() {
    String subject = "/blobs/CSV/ORG123/COOKINGHOBS/file123.csv";
    EventDetails details = service.parseEventSubject(subject);
    assertNotNull(details);
    assertEquals("ORG123", details.getOrgId());
    assertEquals("COOKINGHOBS", details.getCategory());
    assertEquals("file123", details.getProductFileId());
  }

  // Test: subject non conforme restituisce null
  @Test
  void testParseEventSubject_invalidSubject_shouldReturnNull() {
    String subject = "invalid-subject";
    assertNull(service.parseEventSubject(subject));
  }

  // Test: eccezione durante il download imposta stato
  @Test
  void testProcessFileFromStorage_downloadThrowsException_setsEprelError() {
    when(fileStorageClient.download(anyString())).thenThrow(new RuntimeException("boom"));
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));

    StorageEventDTO event = StorageEventDTO.builder()
      .subject("/blobs/CSV/ORG123/COOKINGHOBS/file123.csv")
      .data(StorageEventData.builder().url("/CSV/ORG123/COOKINGHOBS/file123.csv").build())
      .build();

    service.execute(List.of(event), null);
    verify(productFileRepository).save(any());
  }

  // Test: se il file non viene scaricato, viene comunque gestito correttamente
  @Test
  void testProcessFileFromStorage_downloadReturnsNull_setsEprelError() {
    when(fileStorageClient.download(anyString())).thenReturn(null);
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));

    StorageEventDTO event = StorageEventDTO.builder()
      .subject("/blobs/CSV/ORG123/COOKINGHOBS/file123.csv")
      .data(StorageEventData.builder().url("/CSV/ORG123/COOKINGHOBS/file123.csv").build())
      .build();

    service.execute(List.of(event), null);
    verify(productFileRepository).save(any());
  }

  // Test: subject invalido non deve generare eccezioni
  @Test
  void testExecute_invalidSubject_shouldSkip() {
    StorageEventDTO event = StorageEventDTO.builder()
      .subject("invalid_subject")
      .data(StorageEventData.builder().url("/CSV/ORG123/COOKINGHOBS/file.csv").build())
      .build();

    assertDoesNotThrow(() -> service.execute(List.of(event), null));
  }

  // Test: evento con data nulla viene ignorato
  @Test
  void testExecute_eventWithNullData_shouldSkip() {
    StorageEventDTO event = StorageEventDTO.builder()
      .data(null)
      .subject("ORGID_CAT_file.csv")
      .build();

    assertDoesNotThrow(() -> service.execute(List.of(event), null));
  }

  // Test: evento con URL vuoto viene ignorato
  @Test
  void testExecute_eventWithEmptyUrl_shouldSkip() {
    StorageEventData data = StorageEventData.builder().url("").build();

    StorageEventDTO event = StorageEventDTO.builder()
      .subject("ORGID_CAT_file.csv")
      .data(data)
      .build();

    assertDoesNotThrow(() -> service.execute(List.of(event), null));
  }

  // Test: eccezione nel parsing CSV non deve generare errori
  @Test
  void testProcessCsvFromStorage_withExceptionInCsvParsing() {
    ByteArrayOutputStream csvContent = new ByteArrayOutputStream();

    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      utils.when(() -> CsvUtils.readHeader(any(ByteArrayOutputStream.class)))
        .thenThrow(new RuntimeException("CSV read error"));

      assertDoesNotThrow(() -> service.processCsvFromStorage(csvContent, PRODUCT_FILE_ID, "OTHER", ORG_ID));
    }
  }

  @Test
  void testOnError_shouldLogError() {
    Message<String> message = mock(Message.class);
    Throwable throwable = new RuntimeException("Test error");

    assertDoesNotThrow(() -> service.onError(message, throwable));
  }
  @Test
  void testOnDeserializationError_shouldLogDeserializationError() {
    Message<String> message = mock(Message.class);
    Throwable throwable = new RuntimeException("Deserialization failed");

    assertDoesNotThrow(() -> service.onDeserializationError(message, throwable));
  }

  @Test
  void testGetObjectReader_shouldReturnNotNullReader() {
    ObjectReader reader = service.getObjectReader();
    assertNotNull(reader);
  }

}
