package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.enums.UploadCsvStatus;
import it.gov.pagopa.register.dto.operation.*;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.service.validator.ProductFileValidatorService;
import it.gov.pagopa.register.utils.CsvUtils;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductFileServiceTest {

  @Mock
  ProductFileRepository productFileRepository;
  @Mock
  ProductRepository productRepository;
  @Mock
  FileStorageClient fileStorageClient;
  @Mock
  ProductFileValidatorService productFileValidator;

  private ProductFileService productFileService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    productFileService = new ProductFileService(productFileRepository, productRepository, fileStorageClient, productFileValidator);
  }

  @Test
  void testGetFilesByPage_Success() {
    String org = "org";
    Pageable page = PageRequest.of(0,2);
    ProductFile pf1 = ProductFile.builder().id("1").fileName("f1.csv").uploadStatus("OK").category("WASHINGMACHINES").build();
    ProductFile pf2 = ProductFile.builder().id("2").fileName("f2.csv").uploadStatus("OK").category("WASHINGMACHINES").build();
    List<ProductFile> list = List.of(pf1, pf2);
    Page<ProductFile> pg = new PageImpl<>(list, page, list.size());
    when(productFileRepository.findByOrganizationIdAndInitiativeIdAndUploadStatusNot(org, null, UploadCsvStatus.FORMAL_ERROR.name(), page))
      .thenReturn(pg);

    ProductFileResponseDTO resp = productFileService.getFilesByPage(org,null, page);
    assertEquals(2, resp.getContent().size());
    assertEquals("f1.csv", resp.getContent().get(0).getFileName());
    assertEquals("f2.csv", resp.getContent().get(1).getFileName());
    assertEquals(0, resp.getPageNo());
    assertEquals(2, resp.getPageSize());
    assertEquals(2, resp.getTotalElements());
    assertEquals(1, resp.getTotalPages());
    verify(productFileRepository).findByOrganizationIdAndInitiativeIdAndUploadStatusNot(org, null, UploadCsvStatus.FORMAL_ERROR.name(), page);
  }

  @Test
  void testGetFilesByPage_Empty() {
    String org = "org";
    Pageable page = PageRequest.of(0,2);
    Page<ProductFile> pg = new PageImpl<>(List.of(), page, 0);
    when(productFileRepository.findByOrganizationIdAndInitiativeIdAndUploadStatusNot(org, null, UploadCsvStatus.FORMAL_ERROR.name(), page))
      .thenReturn(pg);

    ProductFileResponseDTO resp = productFileService.getFilesByPage(org, null, page);
    assertTrue(resp.getContent().isEmpty());
    assertEquals(0, resp.getTotalElements());
    assertEquals(0, resp.getTotalPages());
  }

  @Test
  void testGetFilesByPage_RepoThrows() {
    Pageable page = PageRequest.of(0,1);
    when(productFileRepository.findByOrganizationIdAndInitiativeIdAndUploadStatusNot(any(), any(), any(), eq(page)))
      .thenThrow(new RuntimeException("DB"));
    RuntimeException ex = assertThrows(RuntimeException.class,
      () -> productFileService.getFilesByPage("org",null, page));
    assertEquals("DB", ex.getMessage());
  }


  @Test
  void downloadReport_partialLoad() throws IOException {
    String productFileId = "1";
    String organizationId = "org1";
    String initiativeId = "TEST_INIT";
    String fileName = "eprel_report.csv";

    ProductFile productFile = ProductFile.builder()
      .id(productFileId)
      .organizationId(organizationId)
      .initiativeId(initiativeId)
      .uploadStatus("PARTIAL")
      .fileName(fileName)
      .build();

    ByteArrayOutputStream mockedOutput = new ByteArrayOutputStream();
    mockedOutput.write("dummy report content".getBytes());

    when(productFileRepository.findByIdAndOrganizationIdAndInitiativeId(productFileId, organizationId, initiativeId))
      .thenReturn(Optional.of(productFile));

    String expectedPath = "Report/Partial/TEST_INIT/1.csv";
    when(fileStorageClient.download(expectedPath)).thenReturn(mockedOutput);

    FileReportDTO reportDTO = productFileService.downloadReport(productFileId, organizationId, initiativeId);

    assertNotNull(reportDTO);
    assertArrayEquals("dummy report content".getBytes(), reportDTO.getData());
    assertEquals("eprel_report_errors.csv", reportDTO.getFilename());

    verify(productFileRepository).findByIdAndOrganizationIdAndInitiativeId(productFileId, organizationId, initiativeId);
  }

  @Test
  void downloadReport_unsupportedStatus() {
    ProductFile pf = ProductFile.builder()
      .id("1")
      .organizationId("o")
      .initiativeId("i")
      .uploadStatus("UNKNOWN")
      .fileName("f.csv")
      .build();

    when(productFileRepository.findByIdAndOrganizationIdAndInitiativeId("1", "o", "i"))
      .thenReturn(Optional.of(pf));

    ReportNotFoundException ex = assertThrows(ReportNotFoundException.class,
      () -> productFileService.downloadReport("1", "o", "i"));

    assertTrue(ex.getMessage().contains("Report not available for file: f.csv"));
  }

  @Test
  void downloadReport_azureNull() {
    ProductFile pf = ProductFile.builder()
      .id("1")
      .organizationId("o")
      .initiativeId("i")
      .uploadStatus("FORMAL_ERROR")
      .build();

    when(productFileRepository.findByIdAndOrganizationIdAndInitiativeId("1", "o", "i"))
      .thenReturn(Optional.of(pf));

    when(fileStorageClient.download("Report/Formal/i/1.csv")).thenReturn(null);

    ReportNotFoundException ex = assertThrows(ReportNotFoundException.class,
      () -> productFileService.downloadReport("1", "o", "i"));

    assertEquals("File not found on storage", ex.getMessage());
  }

  @Test
  void downloadReport_notFound_ShouldLogAndThrowException() {
    String id = "687f8a176a5c92458819922a";
    String orgId = "83843864-f3c0-4def-badb-7f197471b72e";
    String initId = "TEST_INIT";

    Mockito.when(productFileRepository.findByIdAndOrganizationIdAndInitiativeId(id, orgId, initId))
      .thenReturn(Optional.empty());

    ReportNotFoundException exception = assertThrows(ReportNotFoundException.class, () -> {
      productFileService.downloadReport(id, orgId, initId);
    });

    assertEquals("Report not found", exception.getMessage());

    verify(productFileRepository).findByIdAndOrganizationIdAndInitiativeId(id, orgId, initId);

    verifyNoInteractions(fileStorageClient);
  }

  private MultipartFile createMockFile() {
    return new MockMultipartFile("file", "test.csv", "text/csv", "test content".getBytes());
  }
  private MultipartFile createMockFile_InvalidFileType() {
    return new MockMultipartFile("file", "test.test", "text/csv", "test content".getBytes());
  }

  //Test with invalid headers
  @Test
  void whenInvalidFileType_thenReturnKoResult() throws IOException {
    MultipartFile file = createMockFile_InvalidFileType();
    ValidationResultDTO validationResultDTO = new ValidationResultDTO("KO","TEST");
    when(productFileValidator.validateFile(any(),anyString(),any(),any())).thenReturn(validationResultDTO);
    ProductFileResult res = productFileService.uploadFile(file, "cat","ini","org","user","email","orgName");
    assertEquals("KO", res.getStatus());
    assertEquals("TEST", res.getErrorKey());
  }

  //Test con controlli formali falliti
  private void testFormalError(String errorMessage) {
    MultipartFile file = createMockFile();

    try (MockedStatic<CsvUtils> mockedCsv = mockStatic(CsvUtils.class);
         MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {

      mockedCsv.when(() -> CsvUtils.readHeaders(file))
        .thenReturn(List.of("Codice GTIN/EAN", "Codice Prodotto", "Categoria", "Paese di Produzione", "Marca", "Modello"));

      mockedCsv.when(() -> CsvUtils.readCsvRecords(file))
        .thenReturn(List.of(mock(CSVRecord.class)));

      mockedCsv.when(() -> CsvUtils.writeCsvWithErrors(any(), any(), any(), any()))
        .thenAnswer(inv -> null);

      mockedFiles.when(() -> Files.newInputStream(any()))
        .thenReturn(new ByteArrayInputStream("dummy".getBytes()));

      when(fileStorageClient.upload(any(), any(), any())).thenReturn(null);

      when(productFileValidator.validateFile(any(), any(),any(),any()))
        .thenReturn(new ValidationResultDTO("OK", null,List.of(mock(CSVRecord.class)),List.of("Codice GTIN/EAN", "Codice Prodotto", "Categoria", "Paese di Produzione", "Marca", "Modello")));

      CSVRecord invalidRecordLocal = mock(CSVRecord.class);
      List<CSVRecord> invalidRecordsLocal = Collections.singletonList(invalidRecordLocal);
      Map<CSVRecord, String> errorMessagesLocal = new HashMap<>();
      errorMessagesLocal.put(invalidRecordLocal, errorMessage);

      ValidationResultDTO validationResult = new ValidationResultDTO("KO", null, invalidRecordsLocal, errorMessagesLocal);
      when(productFileValidator.validateRecords(any(), any(), any())).thenReturn(validationResult);

      ProductFile savedProductFile = ProductFile.builder().id("123").build();
      when(productFileRepository.save(any())).thenReturn(savedProductFile);

      ProductFileResult result = productFileService.uploadFile(file, "COOKINGHOBS", "ini1","org1", "user1","email","orgName");

      assertEquals("KO", result.getStatus());
      assertEquals(AssetRegisterConstants.UploadKeyConstant.REPORT_FORMAL_FILE_ERROR_KEY, result.getErrorKey());
      assertEquals("123", result.getProductFileId());
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
  }

  @Test
  void whenInvalidGtin_thenReturnFormalError()  {
    testFormalError("Il Codice GTIN/EAN è obbligatorio e deve essere univoco ed alfanumerico e lungo al massimo 14 caratteri");
  }

  @Test
  void whenInvalidProductCode_thenReturnFormalError()  {
    testFormalError("Il Codice prodotto non deve contenere caratteri speciali o lettere accentate e deve essere lungo al massimo 100 caratteri");
  }

  @Test
  void whenInvalidCategory_thenReturnFormalError()  {
    testFormalError("Il campo Categoria è obbligatorio");
  }

  @Test
  void whenInvalidCountry_thenReturnFormalError()  {
    testFormalError("Il Paese di Produzione è obbligatorio e deve essere composto da esattamente 2 caratteri");
  }

  @Test
  void whenInvalidBrand_thenReturnFormalError()  {
    testFormalError("Il campo Marca è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri");
  }

  @Test
  void whenInvalidModel_thenReturnFormalError()  {
    testFormalError("Il campo Modello è obbligatorio e deve contenere una stringa lunga al massimo 100 caratteri");
  }

  @Test
  void whenAllValid_thenReturnOk()  {
    MultipartFile file = mock(MultipartFile.class);
    CSVRecord rec = mock(CSVRecord.class);

    try (MockedStatic<CsvUtils> mocked = mockStatic(CsvUtils.class)) {

      mocked.when(() -> CsvUtils.readHeaders(file))
        .thenReturn(List.of("C1"));
      mocked.when(() -> CsvUtils.readCsvRecords(file))
        .thenReturn(List.of(rec));

      when(productFileValidator.validateFile(file, "cat","ini","org"))
        .thenReturn(ValidationResultDTO.ok(List.of(rec), List.of("C1")));

      when(productFileValidator.validateRecords(List.of(rec), "c1", "cat"))
        .thenReturn(ValidationResultDTO.ok());

      when(productFileRepository.save(any())).thenReturn(ProductFile.builder().id("42").build());

      ByteArrayInputStream bis = new ByteArrayInputStream("abc".getBytes());
      when(file.getInputStream()).thenReturn(bis);
      when(file.getOriginalFilename()).thenReturn("f.csv");
      when(file.getContentType()).thenReturn("text/csv");

      when(fileStorageClient.upload(any(), any(), any())).thenReturn(null);

      ProductFileResult res = productFileService.uploadFile(file, "cat", "ini","org", "user","email","orgName");

      assertEquals("OK", res.getStatus());
      assertNull(res.getErrorKey());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void shouldReturnMappedDTOListWhenValidDataIsPresent() {
    Product file = Product.builder()
      .productFileId("file123")
      .category("DISHWASHERS")
      .build();

    when(productRepository.retrieveDistinctProductFileIdsBasedOnRole("org123", null,null,"operatore"))
      .thenReturn(List.of(file));

    List<ProductBatchDTO> result = productFileService.retrieveDistinctProductFileIdsBasedOnRole("org123", null,null,"operatore");

    assertEquals(1, result.size());
    assertEquals("file123", result.getFirst().getProductFileId());
    assertEquals("Lavastoviglie_file123.csv", result.getFirst().getBatchName());
  }

  @Test
  void whenFileAlreadyInProgressOrUploaded_thenReturnKoAlreadyInProgress() {
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "dummy".getBytes());

    // Simula presenza di un file già in stato IN_PROCESS o UPLOADED
    when(productFileRepository.existsByInitiativeIdAndOrganizationIdAndUploadStatusIn(any(),eq("org"), anyList()))
      .thenReturn(true);

    ProductFileResult result = productFileService.uploadFile(file, "cat", "ini","org", "user", "email","orgName");

    assertEquals("KO", result.getStatus());
    assertEquals(AssetRegisterConstants.UploadKeyConstant.UPLOAD_ALREADY_IN_PROGRESS, result.getErrorKey());
  }


}
