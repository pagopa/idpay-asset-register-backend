package it.gov.pagopa.register.service.consumer;

import it.gov.pagopa.register.configuration.InitiativeConfigMap;
import it.gov.pagopa.register.connector.notification.NotificationServiceImpl;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.dto.operation.StorageEventDTO;
import it.gov.pagopa.register.dto.operation.StorageEventDTO.StorageEventData;
import it.gov.pagopa.register.dto.utils.EventDetails;
import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.event.producer.ProductFileProducer;
import it.gov.pagopa.register.exception.operation.EprelException;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.initiative.CategoryExternalCheck;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.service.validator.product.ValidationService;
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
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductFileConsumerServiceTest {

  @InjectMocks
  private ProductFileConsumerService service;

  @Mock private ProductRepository productRepository;
  @Mock private FileStorageClient fileStorageClient;
  @Mock private ProductFileRepository productFileRepository;
  @Mock private ObjectMapper objectMapper;
  @Mock private NotificationServiceImpl notificationService;
  @Mock private ProductFileProducer productFileProducer;
  @Mock private ConsumerControlService consumerControlService;
  @Mock private InitiativeConfigMap initiativeConfigMap;
  @Mock private ValidationService validationService;
  @Mock private ProducersInitiativeRepository producersInitiativeRepository;

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
      validationService,
      notificationService,
      productFileProducer,
      consumerControlService,
      initiativeConfigMap,
      producersInitiativeRepository
    );
  }

  @Test
  void testExecute_validEvent_shouldProcessFile() {
    mockInitiative("COOKINGHOBS", false);

    StorageEventDTO event = buildEvent("COOKINGHOBS");

    ProducersInitiative producersInitiative = new ProducersInitiative();
    producersInitiative.setEnabled(true);
    producersInitiative.setProducerEmail("test@pagopa.it");
    when(producersInitiativeRepository.findById("ORG123_" + INITIATIVE_ID)).thenReturn(Optional.of(producersInitiative));

    when(fileStorageClient.download(anyString())).thenReturn(new ByteArrayOutputStream());
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));
    when(productRepository.saveAll(any())).thenReturn(List.of());

    when(validationService.validateRecords(
      any(), any(), any(), any(), any(),
      any(), any(), any(), any(), any()))
      .thenReturn(validationResultWithValidProduct());

    mockCsv();

    assertDoesNotThrow(() -> service.execute(List.of(event), null));
  }

  @Test
  void testExecute_shouldHandleEprelError() {
    mockInitiative("WASHINGMACHINES", true);

    StorageEventDTO event = buildEvent("WASHINGMACHINES");

    ProducersInitiative producersInitiative = new ProducersInitiative();
    producersInitiative.setEnabled(true);
    producersInitiative.setProducerEmail("test@pagopa.it");
    when(producersInitiativeRepository.findById("ORG123_" + INITIATIVE_ID)).thenReturn(Optional.of(producersInitiative));

    when(fileStorageClient.download(anyString())).thenReturn(new ByteArrayOutputStream());
    when(productFileRepository.findById(anyString()))
      .thenReturn(Optional.of(new ProductFile()));

    when(validationService.validateRecords(
      any(), any(), any(), any(), any(),
      any(), any(), any(), any(), any()))
      .thenThrow(new EprelException("eprel down"));

    mockCsv();

    service.execute(List.of(event), null);

    verify(consumerControlService).stopConsumer();
    verify(consumerControlService).startEprelHealthCheck();
  }

  @Test
  void testExecute_onlyErrors_shouldNotSaveProducts_butUploadErrorFile() {
    mockInitiative("COOKINGHOBS", false);

    StorageEventDTO event = buildEvent("COOKINGHOBS");

    ProducersInitiative producersInitiative = new ProducersInitiative();
    producersInitiative.setEnabled(true);
    producersInitiative.setProducerEmail("test@pagopa.it");
    when(producersInitiativeRepository.findById("ORG123_" + INITIATIVE_ID)).thenReturn(Optional.of(producersInitiative));

    when(fileStorageClient.download(any())).thenReturn(new ByteArrayOutputStream());
    when(productFileRepository.findById(any()))
      .thenReturn(Optional.of(new ProductFile()));

    CSVRecord errorRecord = mock(CSVRecord.class);

    ProductValidationResult result = new ProductValidationResult(
      Map.of(),
      List.of(errorRecord),
      Map.of(errorRecord, "ERR")
    );

    when(validationService.validateRecords(any(), any(), any(), any(), any(),
      any(), any(), any(), any(), any()))
      .thenReturn(result);

    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      utils.when(() -> CsvUtils.readHeader(any())).thenReturn(List.of("H"));
      utils.when(() -> CsvUtils.readCsvRecords((MultipartFile) any()))
        .thenReturn(List.of(mock(CSVRecord.class)));

      utils.when(() -> CsvUtils.writeCsvWithErrors(any(), any(), any(), any()))
        .thenAnswer(inv -> null);

      service.execute(List.of(event), null);
    }

    verify(productRepository, never()).saveAll(any());
    verify(fileStorageClient).upload(any(), anyString(), eq("text/csv"));
    verify(notificationService).sendEmailPartial(any(), any());
  }

  @Test
  void testExecute_onlyValidProducts_shouldSetLoaded() {
    mockInitiative("COOKINGHOBS", false);

    StorageEventDTO event = buildEvent("COOKINGHOBS");

    ProducersInitiative producersInitiative = new ProducersInitiative();
    producersInitiative.setEnabled(true);
    producersInitiative.setProducerEmail("test@pagopa.it");
    when(producersInitiativeRepository.findById("ORG123_" + INITIATIVE_ID)).thenReturn(Optional.of(producersInitiative));

    when(fileStorageClient.download(any())).thenReturn(new ByteArrayOutputStream());
    when(productFileRepository.findById(any()))
      .thenReturn(Optional.of(new ProductFile()));

    when(productRepository.saveAll(any()))
      .thenReturn(List.of(new Product()));

    when(validationService.validateRecords(any(), any(), any(), any(), any(),
      any(), any(), any(), any(), any()))
      .thenReturn(validationResultWithValidProduct());

    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      utils.when(() -> CsvUtils.readHeader(any())).thenReturn(List.of("H"));
      utils.when(() -> CsvUtils.readCsvRecords((MultipartFile) any()))
        .thenReturn(List.of(mock(CSVRecord.class)));

      service.execute(List.of(event), null);
    }

    verify(productRepository).saveAll(any());
    verify(notificationService).sendEmailOk(any(), any());
    verify(fileStorageClient, never()).upload(any(), anyString(), any());
  }

  @Test
  void testExecute_validAndInvalidProducts_shouldSetPartialAndUploadErrorFile() {
    mockInitiative("COOKINGHOBS", false);

    StorageEventDTO event = buildEvent("COOKINGHOBS");

    ProducersInitiative producersInitiative = new ProducersInitiative();
    producersInitiative.setEnabled(true);
    producersInitiative.setProducerEmail("test@pagopa.it");
    when(producersInitiativeRepository.findById("ORG123_" + INITIATIVE_ID)).thenReturn(Optional.of(producersInitiative));

    when(fileStorageClient.download(any())).thenReturn(new ByteArrayOutputStream());
    when(productFileRepository.findById(any()))
      .thenReturn(Optional.of(new ProductFile()));

    when(productRepository.saveAll(any()))
      .thenReturn(List.of(new Product()));

    CSVRecord errorRecord = mock(CSVRecord.class);

    ProductValidationResult result = new ProductValidationResult(
      Map.of("k", new Product()),
      List.of(errorRecord),
      Map.of(errorRecord, "ERR")
    );

    when(validationService.validateRecords(
      any(), any(), any(), any(), any(),
      any(), any(), any(), any(), any()))
      .thenReturn(result);

    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      utils.when(() -> CsvUtils.readHeader(any())).thenReturn(List.of("H"));
      utils.when(() -> CsvUtils.readCsvRecords((MultipartFile) any()))
        .thenReturn(List.of(mock(CSVRecord.class)));

      utils.when(() -> CsvUtils.writeCsvWithErrors(any(), any(), any(), any()))
        .thenAnswer(inv -> null);

      service.execute(List.of(event), null);
    }

    verify(productRepository).saveAll(any());
    verify(fileStorageClient).upload(any(), anyString(), eq("text/csv"));
    verify(notificationService).sendEmailPartial(any(), any());
  }

  @Test
  void testExecute_organizationNotEnabled_shouldSetPartial() {
    mockInitiative("COOKINGHOBS", false);

    StorageEventDTO event = buildEvent("COOKINGHOBS");

    ProducersInitiative producersInitiative = new ProducersInitiative();
    producersInitiative.setEnabled(false);
    when(producersInitiativeRepository.findById("ORG123_" + INITIATIVE_ID)).thenReturn(Optional.of(producersInitiative));

    when(fileStorageClient.download(any())).thenReturn(new ByteArrayOutputStream());
    when(productFileRepository.findById(any())).thenReturn(Optional.of(new ProductFile()));

    when(validationService.validateRecords(any(), any(), any(), any(), any(),
      any(), any(), any(), any(), any()))
      .thenReturn(validationResultWithValidProduct());

    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      utils.when(() -> CsvUtils.readHeader(any())).thenReturn(List.of("H"));
      utils.when(() -> CsvUtils.readCsvRecords((MultipartFile) any()))
        .thenReturn(List.of(mock(CSVRecord.class)));

      assertDoesNotThrow(() -> service.execute(List.of(event), null));
    }

    verify(productRepository).saveAll(any());
  }

  @Test
  void testExecute_missingOperativeEmail_shouldSetPartial() {
    mockInitiative("COOKINGHOBS", false);

    StorageEventDTO event = buildEvent("COOKINGHOBS");

    ProducersInitiative producersInitiative = new ProducersInitiative();
    producersInitiative.setEnabled(true);
    producersInitiative.setProducerEmail(null);
    when(producersInitiativeRepository.findById("ORG123_" + INITIATIVE_ID)).thenReturn(Optional.of(producersInitiative));

    when(fileStorageClient.download(any())).thenReturn(new ByteArrayOutputStream());
    when(productFileRepository.findById(any())).thenReturn(Optional.of(new ProductFile()));

    when(validationService.validateRecords(any(), any(), any(), any(), any(),
      any(), any(), any(), any(), any()))
      .thenReturn(validationResultWithValidProduct());

    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      utils.when(() -> CsvUtils.readHeader(any())).thenReturn(List.of("H"));
      utils.when(() -> CsvUtils.readCsvRecords((MultipartFile) any()))
        .thenReturn(List.of(mock(CSVRecord.class)));

      assertDoesNotThrow(() -> service.execute(List.of(event), null));
    }

    verify(productRepository).saveAll(any());
  }

  @Test
  void testSetProductFileStatus_fileNotFound_shouldLogWarn() {
    when(productFileRepository.findById("UNKNOWN_FILE_ID"))
      .thenReturn(Optional.empty());

    assertDoesNotThrow(() -> service.setProductFileStatus("UNKNOWN_FILE_ID", "PARTIAL", 0));

    verify(productFileRepository, never()).save(any());
  }

  @Test
  void testParseEventSubject_valid() {
    EventDetails details = service.parseEventSubject(
      "/blobs/CSV/" + INITIATIVE_ID + "/ORG123/ORGNAME/COOKINGHOBS/file123.csv"
    );

    assertNotNull(details);
    assertEquals("ORG123", details.getOrgId());
  }

  @Test
  void testParseEventSubject_invalid() {
    assertNull(service.parseEventSubject("invalid"));
  }

  @Test
  void testExtractBlobPath_invalidUrl() {
    assertNull(service.extractBlobPath("/wrongprefix/file.csv"));
  }

  @Test
  void testOnError() {
    assertDoesNotThrow(() ->
      service.onError(mock(Message.class), new RuntimeException()));
  }

  private StorageEventDTO buildEvent(String category) {
    String url = "/CSV/" + INITIATIVE_ID + "/ORG123/ORGNAME/" + category + "/file123.csv";

    return StorageEventDTO.builder()
      .subject("/blobs" + url)
      .data(StorageEventData.builder().url(url).build())
      .build();
  }

  private void mockCsv() {
    try (MockedStatic<CsvUtils> utils = mockStatic(CsvUtils.class)) {
      utils.when(() -> CsvUtils.readHeader(any()))
        .thenReturn(List.of("HEADER"));

      utils.when(() -> CsvUtils.readCsvRecords((MultipartFile) any()))
        .thenReturn(List.of(mock(CSVRecord.class)));
    }
  }

  private void mockInitiative(String category, boolean withExternalChecks) {
    CategoryConfig categoryConfig = new CategoryConfig(
      "TEMPLATE",
      "GTIN",
      withExternalChecks
        ? List.of(new CategoryExternalCheck("EPREL", Map.of()))
        : List.of(),
      "EPREL"
    );

    InitiativeConfig initiativeConfig = new InitiativeConfig();
    initiativeConfig.setCategories(Map.of(category, categoryConfig));
    initiativeConfig.setAllowedReloadStatuses(List.of("VALID"));

    when(initiativeConfigMap.get(anyString()))
      .thenReturn(initiativeConfig);
  }

  private ProductValidationResult validationResultWithValidProduct() {
    return new ProductValidationResult(
      Map.of("model123", new Product()),
      List.of(),
      Map.of()
    );
  }
}
