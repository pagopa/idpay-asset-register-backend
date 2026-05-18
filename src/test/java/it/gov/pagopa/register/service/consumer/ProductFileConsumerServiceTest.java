package it.gov.pagopa.register.service.consumer;

import com.azure.storage.blob.models.BlobStorageException;
import it.gov.pagopa.register.configuration.InitiativeConfigMap;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.initiative.CategoryExternalCheck;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import it.gov.pagopa.register.connector.notification.NotificationServiceImpl;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.dto.operation.StorageEventDTO;
import it.gov.pagopa.register.dto.operation.StorageEventDTO.StorageEventData;
import it.gov.pagopa.register.dto.utils.EventDetails;
import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.event.producer.ProductFileProducer;
import it.gov.pagopa.register.exception.operation.EprelException;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.service.validator.external.check.ExternalCheckService;
import it.gov.pagopa.register.service.validator.nocheck.NoExternalCheckService;
import it.gov.pagopa.register.utils.CsvUtils;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
  private ObjectMapper objectMapper;
  @Mock
  private NotificationServiceImpl notificationService;
  @Mock
  private ProductFileProducer productFileProducer;
  @Mock
  private ConsumerControlService consumerControlService;
  @Mock
  private InitiativeConfigMap initiativeConfigMap;
  @Mock
  private ExternalCheckService externalCheckService;
  @Mock
  private NoExternalCheckService noExternalCheckService;
  private static final String ORG_ID = "ORG123";
  private static final String PRODUCT_FILE_ID = "file123";
  private static final String INITIATIVE_ID = "687f8a176a5c92458819922a";


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
      noExternalCheckService,
      externalCheckService,
      notificationService,
      productFileProducer,
      consumerControlService,
      initiativeConfigMap);
  }



  @Test
  void testExecute_validEvent_shouldProcessFile_Cookinghobs() {
    StorageEventData data = StorageEventData.builder()
      .url("/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/COOKINGHOBS/file123.csv")
      .build();
    StorageEventDTO event = StorageEventDTO.builder()
      .subject("/blobs/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/COOKINGHOBS/file123.csv")
      .data(data)
      .build();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    when(fileStorageClient.download(anyString())).thenReturn(stream);
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));
    when(productRepository.saveAll(any())).thenReturn(List.of());
    mockInitiative("COOKINGHOBS", false);
    when(noExternalCheckService.validateRecords(any(), any(), any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(validationResultWithValidProduct());


    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      CSVRecord csvRecord = mock(CSVRecord.class);
      utils.when(() -> CsvUtils.readHeader(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of("HEADER"));
      utils.when(() -> CsvUtils.readCsvRecords(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of(csvRecord));
      assertDoesNotThrow(() -> service.execute(List.of(event), null));
    }}


  @Test
  void testExecute_validEvent_shouldProcessFile_Eprel() {
    StorageEventData data = StorageEventData.builder()
      .url("/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/WASHINGMACHINES/file123.csv")
      .build();
    StorageEventDTO event = StorageEventDTO.builder()
      .subject("/blobs/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/WASHINGMACHINES/file123.csv")
      .data(data)
      .build();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    when(fileStorageClient.download(anyString())).thenReturn(stream);
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));
    when(productRepository.saveAll(any())).thenReturn(List.of());
    mockInitiative("WASHINGMACHINES", true);
    when(externalCheckService.validateRecords(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(validationResultWithValidProduct());


    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      CSVRecord csvRecord = mock(CSVRecord.class);
      utils.when(() -> CsvUtils.readHeader(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of("HEADER"));
      utils.when(() -> CsvUtils.readCsvRecords(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of(csvRecord));
      assertDoesNotThrow(() -> service.execute(List.of(event), null));
    }}

  @Test
  void testExecute_validEvent_shouldProcessFile_butThrowEprelError() {
    StorageEventData data = StorageEventData.builder()
      .url("/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/WASHINGMACHINES/file123.csv")
      .build();
    StorageEventDTO event = StorageEventDTO.builder()
      .subject("/blobs/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/WASHINGMACHINES/file123.csv")
      .data(data)
      .build();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    when(fileStorageClient.download(anyString())).thenReturn(stream);
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));
    mockInitiative("WASHINGMACHINES", true);
    when(externalCheckService.validateRecords(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
      .thenThrow(new EprelException("eprel down"));

    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      CSVRecord csvRecord = mock(CSVRecord.class);
      utils.when(() -> CsvUtils.readHeader(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of("HEADER"));
      utils.when(() -> CsvUtils.readCsvRecords(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of(csvRecord));
    }

    service.execute(List.of(event), null);
    verify(consumerControlService).stopConsumer();
    verify(consumerControlService).startEprelHealthCheck();
  }

  @Test
  void testExecute_validEvent_shouldProcessFile_butThrowEprelErrorAndJSONException() throws JacksonException {
    StorageEventData data = StorageEventData.builder()
      .url("/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/WASHINGMACHINES/file123.csv")
      .build();
    StorageEventDTO event = StorageEventDTO.builder()
      .subject("/blobs/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/WASHINGMACHINES/file123.csv")
      .data(data)
      .build();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    when(fileStorageClient.download(anyString())).thenReturn(stream);
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));
    mockInitiative("WASHINGMACHINES", true);
    when(externalCheckService.validateRecords(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
      .thenThrow(new EprelException("eprel down"));

    when(objectMapper.writeValueAsString(any())).thenThrow(new JacksonException("Error during serialization") {});
    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      CSVRecord csvRecord = mock(CSVRecord.class);
      utils.when(() -> CsvUtils.readHeader(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of("HEADER"));
      utils.when(() -> CsvUtils.readCsvRecords(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of(csvRecord));
    }

    service.execute(List.of(event), null);
    verify(consumerControlService).stopConsumer();
  }
  @Test
  void testExecute_validEvent_shouldProcessFile_Eprel_InvalidRecords() {
    StorageEventData data = StorageEventData.builder()
      .url("/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/WASHINGMACHINES/file123.csv")
      .build();
    StorageEventDTO event = StorageEventDTO.builder()
      .subject("/blobs/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/WASHINGMACHINES/file123.csv")
      .data(data)
      .build();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    when(fileStorageClient.download(anyString())).thenReturn(stream);
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));
    when(productRepository.saveAll(any())).thenReturn(List.of());
    CSVRecord record1 = mock(CSVRecord.class);
    CSVRecord record2 = mock(CSVRecord.class);

    List<CSVRecord> invalidRecords = List.of(record1, record2);
    Map<CSVRecord, String> errorMessages = new HashMap<>();
    mockInitiative("WASHINGMACHINES", true);
    when(externalCheckService.validateRecords(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(new ProductValidationResult(Map.of("model123", new Product()), invalidRecords, errorMessages));


    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      CSVRecord csvRecord = mock(CSVRecord.class);
      utils.when(() -> CsvUtils.readHeader(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of("HEADER"));
      utils.when(() -> CsvUtils.readCsvRecords(any(ByteArrayOutputStream.class)))
        .thenReturn(List.of(csvRecord));
      assertDoesNotThrow(() -> service.execute(List.of(event), null));
    }}

  @Test
  void testExtractBlobPath_invalidUrl_shouldReturnNull() {
    String url = "/wrongprefix/file.csv";
    assertNull(service.extractBlobPath(url));
  }

  @Test
  void testSetProductFileStatus_fileNotFound_shouldDoNothing() {
    when(productFileRepository.findById("missing-file")).thenReturn(Optional.empty());
    service.setProductFileStatus("missing-file", "PARTIAL", 0);
    verify(productFileRepository, never()).save(any());
  }

  @Test
  void testParseEventSubject_validSubject_shouldReturnDetails() {
    String subject = "/blobs/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/COOKINGHOBS/file123.csv";
    EventDetails details = service.parseEventSubject(subject);
    assertNotNull(details);
    assertEquals("ORG123", details.getOrgId());
    assertEquals(INITIATIVE_ID, details.getInitiativeId());
    assertEquals("COOKINGHOBS", details.getCategory());
    assertEquals("file123", details.getProductFileId());
  }

  @Test
  void testParseEventSubject_invalidSubject_shouldReturnNull() {
    String subject = "invalid-subject";
    assertNull(service.parseEventSubject(subject));
  }

  @Test
  void testProcessFileFromStorage_downloadThrowsException_setsEprelError() {
    when(fileStorageClient.download(anyString())).thenThrow(new BlobStorageException(null,null,null));
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));

    StorageEventDTO event = StorageEventDTO.builder()
      .subject("/blobs/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/COOKINGHOBS/file123.csv")
      .data(StorageEventData.builder().url("/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/COOKINGHOBS/file123.csv").build())
      .build();

    service.execute(List.of(event), null);
    verify(productFileRepository).save(any());
  }

  @Test
  void testProcessFileFromStorage_downloadReturnsNull_setsEprelError() {
    when(fileStorageClient.download(anyString())).thenReturn(null);
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));

    StorageEventDTO event = StorageEventDTO.builder()
      .subject("/blobs/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/COOKINGHOBS/file123.csv")
      .data(StorageEventData.builder().url("/CSV/687f8a176a5c92458819922a/ORG123/ORGNAME/COOKINGHOBS/file123.csv").build())
      .build();

    service.execute(List.of(event), null);
    verify(productFileRepository).save(any());
  }

  @Test
  void testExecute_invalidSubject_shouldSkip() {
    StorageEventDTO event = StorageEventDTO.builder()
      .subject("invalid_subject")
      .data(StorageEventData.builder().url("/CSV/687f8a176a5c92458819922a/ORG123/COOKINGHOBS/file.csv").build())
      .build();

    assertDoesNotThrow(() -> service.execute(List.of(event), null));
  }

  @Test
  void testExecute_eventWithNullData_shouldSkip() {
    StorageEventDTO event = StorageEventDTO.builder()
      .data(null)
      .subject("ORGID_CAT_file.csv")
      .build();

    assertDoesNotThrow(() -> service.execute(List.of(event), null));
  }

  @Test
  void testExecute_eventWithEmptyUrl_shouldSkip() {
    StorageEventData data = StorageEventData.builder().url("").build();

    StorageEventDTO event = StorageEventDTO.builder()
      .subject("ORGID_CAT_file.csv")
      .data(data)
      .build();

    assertDoesNotThrow(() -> service.execute(List.of(event), null));
  }

  @Test
  void testProcessCsvFromStorage_withExceptionInCsvParsing() {
    ByteArrayOutputStream csvContent = new ByteArrayOutputStream();

    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      utils.when(() -> CsvUtils.readHeader(any(ByteArrayOutputStream.class)))
        .thenThrow(new IOException("CSV read error"));

      assertDoesNotThrow(() -> service.processCsvFromStorage(csvContent, PRODUCT_FILE_ID, "OTHER", ORG_ID, "ORG_NAME","INI_ID"));
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

  private void mockInitiative(String category, boolean withExternalChecks) {
    CategoryConfig categoryConfig = new CategoryConfig(
      "TEMPLATE",
      "Codice GTIN/EAN",
      withExternalChecks ? List.of(new CategoryExternalCheck("EPREL", Map.of())) : List.of(),
      "EPREL"
    );
    InitiativeConfig initiativeConfig = new InitiativeConfig();
    initiativeConfig.setCategories(Map.of(category, categoryConfig));
    initiativeConfig.setAllowedReloadStatuses(List.of());
    when(initiativeConfigMap.get(INITIATIVE_ID)).thenReturn(initiativeConfig);
  }

  private ProductValidationResult validationResultWithValidProduct() {
    return new ProductValidationResult(Map.of("model123", new Product()), List.of(), Map.of());
  }

}
