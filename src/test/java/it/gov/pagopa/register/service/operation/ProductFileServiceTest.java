package it.gov.pagopa.register.service.operation;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.IntStream;

import it.gov.pagopa.common.storage.AzureBlobClientImpl;
import it.gov.pagopa.register.config.ProductFileValidationConfig;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.constants.AssetRegisterConstant;
import it.gov.pagopa.register.constants.enums.UploadCsvStatus;
import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResult;
import it.gov.pagopa.register.dto.operation.ValidationResultDTO;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.utils.CsvUtils;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ProductFileServiceTest {

  @Mock ProductFileRepository productFileRepository;
  @Mock FileStorageClient fileStorageClient;
  @Mock AzureBlobClientImpl azureBlobClient;
  @Mock ProductFileValidator productFileValidator;
  @Mock ProductFileValidationConfig validationConfig;

  private ProductFileService productFileService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    productFileService = new ProductFileService(productFileRepository, fileStorageClient, productFileValidator);
    // in Jetty container spring config getter exist
    ReflectionTestUtils.setField(productFileService, "validationConfig", validationConfig);
  }

  private MultipartFile createMockFile() {
    return new MockMultipartFile("file", "test.csv", "text/csv", "test content".getBytes());
  }

  //--------------------------Test per metodo getFilesByPage----------------------------------------
  //Test con esito positivo dell'api
  @Test
  void testGetFilesByPage_Success() {
    String org = "org";
    Pageable page = PageRequest.of(0,2);
    ProductFile pf1 = ProductFile.builder().id("1").fileName("f1.csv").uploadStatus("OK").build();
    ProductFile pf2 = ProductFile.builder().id("2").fileName("f2.csv").uploadStatus("OK").build();
    List<ProductFile> list = List.of(pf1, pf2);
    Page<ProductFile> pg = new PageImpl<>(list, page, list.size());
    when(productFileRepository.findByOrganizationIdAndUploadStatusNot(org, UploadCsvStatus.FORMAL_ERROR.name(), page))
      .thenReturn(pg);

    ProductFileResponseDTO resp = productFileService.getFilesByPage(org, page);
    assertEquals(2, resp.getContent().size());
    assertEquals("f1.csv", resp.getContent().get(0).getFileName());
    assertEquals("f2.csv", resp.getContent().get(1).getFileName());
    assertEquals(0, resp.getPageNo());
    assertEquals(2, resp.getPageSize());
    assertEquals(2, resp.getTotalElements());
    assertEquals(1, resp.getTotalPages());

    verify(productFileRepository).findByOrganizationIdAndUploadStatusNot(org, UploadCsvStatus.FORMAL_ERROR.name(), page);
  }


  //Test con nessun risultato dell'api
  @Test
  void testGetFilesByPage_Empty() {
    String org = "org";
    Pageable page = PageRequest.of(0,2);
    Page<ProductFile> pg = new PageImpl<>(List.of(), page, 0);
    when(productFileRepository.findByOrganizationIdAndUploadStatusNot(org, UploadCsvStatus.FORMAL_ERROR.name(), page))
      .thenReturn(pg);

    ProductFileResponseDTO resp = productFileService.getFilesByPage(org, page);
    assertTrue(resp.getContent().isEmpty());
    assertEquals(0, resp.getTotalElements());
    assertEquals(0, resp.getTotalPages());
  }


  //Test con eccezioni da parte del repository
  @Test
  void testGetFilesByPage_RepoThrows() {
    Pageable page = PageRequest.of(0,1);
    when(productFileRepository.findByOrganizationIdAndUploadStatusNot(any(), any(), eq(page)))
      .thenThrow(new RuntimeException("DB"));
    RuntimeException ex = assertThrows(RuntimeException.class,
      () -> productFileService.getFilesByPage("org", page));
    assertEquals("DB", ex.getMessage());
  }


  //Test con mancato organizationId
  @Test
  void testGetFilesByPage_NullOrg() {
    Pageable page = PageRequest.of(0,1);
    assertThrows(IllegalArgumentException.class, () -> productFileService.getFilesByPage(null, page));
  }

  //-------------------------Test su metodo downloadReport--------------------

  //Test con errori Eprel
  @Test
  void downloadReport_eprelError() {
    ProductFile pf = new ProductFile(); pf.setId("1"); pf.setOrganizationId("o"); pf.setUploadStatus("EPREL_ERROR");
    when(productFileRepository.findByIdAndOrganizationId("1","o")).thenReturn(Optional.of(pf));
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    when(azureBlobClient.download("Report/Eprel_Error/1.csv")).thenReturn(os);

    ByteArrayOutputStream res = productFileService.downloadReport("1","o");
    assertSame(os, res);
  }

  //Test con errori formali
  @Test
  void downloadReport_formalError() {
    ProductFile pf = new ProductFile(); pf.setId("1"); pf.setOrganizationId("o"); pf.setUploadStatus("FORMAL_ERROR");
    when(productFileRepository.findByIdAndOrganizationId("1","o")).thenReturn(Optional.of(pf));
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    when(azureBlobClient.download("Report/Formal_Error/1.csv")).thenReturn(os);

    ByteArrayOutputStream res = productFileService.downloadReport("1","o");
    assertSame(os, res);
  }

  //Test con idUpload errato -> ritorna un'eccezione
  @Test
  void downloadReport_notFoundId() {
    when(productFileRepository.findByIdAndOrganizationId(any(), any())).thenReturn(Optional.empty());
    ReportNotFoundException ex = assertThrows(ReportNotFoundException.class,
      () -> productFileService.downloadReport("1","o"));
    assertTrue(ex.getMessage().contains("Report not found with id: 1"));
  }

  //Test con status errato -> ritorna un'eccezione
  @Test
  void downloadReport_unsupportedStatus() {
    ProductFile pf = new ProductFile(); pf.setId("1"); pf.setOrganizationId("o"); pf.setUploadStatus("UNKNOWN"); pf.setFileName("f");
    when(productFileRepository.findByIdAndOrganizationId("1","o")).thenReturn(Optional.of(pf));
    ReportNotFoundException ex = assertThrows(ReportNotFoundException.class,
      () -> productFileService.downloadReport("1","o"));
    assertTrue(ex.getMessage().contains("Report not available for file: f"));
  }

  //Test quando Azure fallisce -> ritorna un'eccezione
  @Test
  void downloadReport_azureNull() {
    ProductFile pf = new ProductFile(); pf.setId("1"); pf.setOrganizationId("o"); pf.setUploadStatus("FORMAL_ERROR");
    when(productFileRepository.findByIdAndOrganizationId("1","o")).thenReturn(Optional.of(pf));
    when(azureBlobClient.download("Report/Formal_Error/1.csv")).thenReturn(null);
    ReportNotFoundException ex = assertThrows(ReportNotFoundException.class,
      () -> productFileService.downloadReport("1","o"));
    assertTrue(ex.getMessage().contains("Report not found on Azure"));
  }


  //-------------------------Test processFile method--------------------

  //Test with invalid headers
  @Test
  void whenInvalidHeaders_thenReturnKoResult() throws Exception {
    MultipartFile file = createMockFile();
    try (MockedStatic<CsvUtils> mocked = mockStatic(CsvUtils.class)) {
      mocked.when(() -> CsvUtils.readHeader(file)).thenReturn(List.of("X","Y"));
      mocked.when(() -> CsvUtils.readCsvRecords(file)).thenReturn(List.of());
      when(productFileValidator.validateFile(file, "cat", List.of("X","Y"), 0))
        .thenReturn(ValidationResultDTO.ko("INVALID_HEADERS"));

      ProductFileResult res = productFileService.processFile(file, "cat","org","user");
      assertEquals("KO", res.getStatus());
      assertEquals("INVALID_HEADERS", res.getErrorKey());
    }
  }


  //Test superati i 100 record
  @Test
  void whenExceedsMaxRows_thenReturnKoResult() throws Exception {
    MultipartFile file = createMockFile();
    List<CSVRecord> recs = new ArrayList<>();
    for (int i=0; i<101; i++) recs.add(mock(CSVRecord.class));
    try (MockedStatic<CsvUtils> mocked = mockStatic(CsvUtils.class)) {
      mocked.when(() -> CsvUtils.readHeader(file))
        .thenReturn(List.of("A","B"));
      mocked.when(() -> CsvUtils.readCsvRecords(file))
        .thenReturn(recs);
      when(productFileValidator.validateFile(file, "cat", List.of("A","B"), 101))
        .thenReturn(ValidationResultDTO.ko("MAX_ROWS_EXCEEDED"));

      ProductFileResult res = productFileService.processFile(file, "cat","org","user");
      assertEquals("KO", res.getStatus());
      assertEquals("MAX_ROWS_EXCEEDED", res.getErrorKey());
    }
  }


  //Test con controlli formali falliti
  private void testFormalError(String errorMessage) throws Exception {
    MultipartFile file = createMockFile();

    try (MockedStatic<CsvUtils> mockedCsv = mockStatic(CsvUtils.class);
         MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {

      mockedCsv.when(() -> CsvUtils.readHeader(file))
        .thenReturn(List.of("Codice GTIN/EAN", "Codice Prodotto", "Categoria", "Paese di Produzione", "Marca", "Modello"));

      mockedCsv.when(() -> CsvUtils.readCsvRecords(file))
        .thenReturn(List.of(mock(CSVRecord.class)));

      mockedCsv.when(() -> CsvUtils.writeCsvWithErrors(any(), any(), any(), anyString()))
        .thenAnswer(inv -> null);

      mockedFiles.when(() -> Files.newInputStream(any()))
        .thenReturn(new ByteArrayInputStream("dummy".getBytes()));

      when(azureBlobClient.upload(any(), any(), any())).thenReturn(null);

      when(productFileValidator.validateFile(any(), any(), any(), anyInt()))
        .thenReturn(new ValidationResultDTO("OK", null));

      CSVRecord invalidRecordLocal = mock(CSVRecord.class);
      List<CSVRecord> invalidRecordsLocal = Arrays.asList(invalidRecordLocal);
      Map<CSVRecord, String> errorMessagesLocal = new HashMap<>();
      errorMessagesLocal.put(invalidRecordLocal, errorMessage);

      ValidationResultDTO validationResult = new ValidationResultDTO("KO", null, invalidRecordsLocal, errorMessagesLocal);
      when(productFileValidator.validateRecords(any(), any(), any())).thenReturn(validationResult);

      ProductFile savedProductFile = ProductFile.builder().id("123").build();
      when(productFileRepository.save(any())).thenReturn(savedProductFile);

      ProductFileResult result = productFileService.processFile(file, "cookinghobs", "org1", "user1");

      assertEquals("KO", result.getStatus());
      assertEquals(AssetRegisterConstant.UploadKeyConstant.REPORT_FORMAL_FILE_ERROR_KEY, result.getErrorKey());
      assertEquals("123", result.getProductFileId());
    }
  }

  @Test
  void whenInvalidGtin_thenReturnFormalError() throws Exception {
    testFormalError("Il Codice GTIN/EAN è obbligatorio e deve essere univoco ed alfanumerico e lungo al massimo 14 caratteri");
  }

  @Test
  void whenInvalidProductCode_thenReturnFormalError() throws Exception {
    testFormalError("Il Codice prodotto non deve contenere caratteri speciali o lettere accentate e deve essere lungo al massimo 100 caratteri");
  }

  @Test
  void whenInvalidCategory_thenReturnFormalError() throws Exception {
    testFormalError("Il campo Categoria è obbligatorio");
  }

  @Test
  void whenInvalidCountry_thenReturnFormalError() throws Exception {
    testFormalError("Il Paese di Produzione è obbligatorio e deve essere composto da esattamente 2 caratteri");
  }

  @Test
  void whenInvalidBrand_thenReturnFormalError() throws Exception {
    testFormalError("Il campo Marca è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri");
  }

  @Test
  void whenInvalidModel_thenReturnFormalError() throws Exception {
    testFormalError("Il campo Modello è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri");
  }

  @Test
  void whenAllValid_thenReturnOk() throws Exception {
    MultipartFile file = createMockFile();
    CSVRecord rec = mock(CSVRecord.class);

    try (MockedStatic<CsvUtils> mocked = mockStatic(CsvUtils.class)) {

      mocked.when(() -> CsvUtils.readHeader(file))
        .thenReturn(List.of("C1"));
      mocked.when(() -> CsvUtils.readCsvRecords(file))
        .thenReturn(List.of(rec));

      when(productFileValidator.validateFile(file, "cat", List.of("C1"), 1))
        .thenReturn(ValidationResultDTO.ok());

      when(productFileValidator.validateRecords(List.of(rec), List.of("C1"), "cat"))
        .thenReturn(ValidationResultDTO.ok());

      when(productFileRepository.save(any())).thenReturn(ProductFile.builder().id("42").build());

      ByteArrayInputStream bis = new ByteArrayInputStream("abc".getBytes());
      when(file.getInputStream()).thenReturn(bis);
      when(file.getOriginalFilename()).thenReturn("f.csv");
      when(file.getContentType()).thenReturn("text/csv");

      when(azureBlobClient.upload(any(), any(), any())).thenReturn(null);

      ProductFileResult res = productFileService.processFile(file, "cat", "org", "user");

      assertEquals("OK", res.getStatus());
      assertNull(res.getErrorKey());
    }
  }
}
