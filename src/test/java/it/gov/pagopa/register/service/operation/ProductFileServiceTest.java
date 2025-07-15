package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.constants.enums.UploadCsvStatus;
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

  @Test
  void testGetFilesByPage_RepoThrows() {
    Pageable page = PageRequest.of(0,1);
    when(productFileRepository.findByOrganizationIdAndUploadStatusNot(any(), any(), eq(page)))
      .thenThrow(new RuntimeException("DB"));
    RuntimeException ex = assertThrows(RuntimeException.class,
      () -> productFileService.getFilesByPage("org", page));
    assertEquals("DB", ex.getMessage());
  }

  @Test
  void testGetFilesByPage_NullOrg() {
    Pageable page = PageRequest.of(0,1);
    assertThrows(IllegalArgumentException.class, () -> productFileService.getFilesByPage(null, page));
  }

  @Test
  void downloadReport_partialLoad() throws IOException {
    ProductFile pf = new ProductFile();
    pf.setId("1");
    pf.setOrganizationId("o");
    pf.setUploadStatus("PARTIAL");
    when(productFileRepository.findByIdAndOrganizationId("1", "o")).thenReturn(Optional.of(pf));

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    os.write("fake csv content".getBytes());
    when(fileStorageClient.download("Report/Partial/1.csv")).thenReturn(os);
    FileReportDTO res = productFileService.downloadReport("1", "o");
    assertArrayEquals(os.toByteArray(), res.getData());


    String productFileId = "1";
    String organizationId = "org1";
    String fileName = "eprel_report.csv";

    ProductFile productFile = ProductFile.builder()
      .id(productFileId)
      .organizationId(organizationId)
      .uploadStatus("PARTIAL")
      .fileName(fileName)
      .build();

    ByteArrayOutputStream mockedOutput = new ByteArrayOutputStream();
    mockedOutput.write("dummy report content".getBytes());

    when(productFileRepository.findByIdAndOrganizationId(productFileId, organizationId))
      .thenReturn(Optional.of(productFile));

    when(fileStorageClient.download("Report/Partial/1.csv"))
      .thenReturn(mockedOutput);

    FileReportDTO reportDTO = productFileService.downloadReport(productFileId, organizationId);

    // Assert
    assertNotNull(reportDTO);
    assertArrayEquals("dummy report content".getBytes(), reportDTO.getData());
    assertEquals("eprel_report_errors.csv", reportDTO.getFilename());

    verify(productFileRepository).findByIdAndOrganizationId(productFileId, organizationId);
    verify(fileStorageClient).download("Report/Partial/1.csv");
  }

  @Test
  void downloadReport_notFoundId() {
    when(productFileRepository.findByIdAndOrganizationId(any(), any())).thenReturn(Optional.empty());
    ReportNotFoundException ex = assertThrows(ReportNotFoundException.class,
      () -> productFileService.downloadReport("1","o"));
    assertTrue(ex.getMessage().contains("Report not found with id: 1"));
  }

  @Test
  void downloadReport_unsupportedStatus() {
    ProductFile pf = new ProductFile(); pf.setId("1"); pf.setOrganizationId("o"); pf.setUploadStatus("UNKNOWN"); pf.setFileName("f");
    when(productFileRepository.findByIdAndOrganizationId("1","o")).thenReturn(Optional.of(pf));
    ReportNotFoundException ex = assertThrows(ReportNotFoundException.class,
      () -> productFileService.downloadReport("1","o"));
    assertTrue(ex.getMessage().contains("Report not available for file: f"));
  }

  @Test
  void downloadReport_azureNull() {
    ProductFile pf = new ProductFile(); pf.setId("1"); pf.setOrganizationId("o"); pf.setUploadStatus("FORMAL_ERROR");
    when(productFileRepository.findByIdAndOrganizationId("1","o")).thenReturn(Optional.of(pf));
    when(fileStorageClient.download("Report/Formal/1.csv")).thenReturn(null);
    ReportNotFoundException ex = assertThrows(ReportNotFoundException.class,
      () -> productFileService.downloadReport("1","o"));
    assertTrue(ex.getMessage().contains("Report not found on Azure"));
  }

  private MultipartFile createMockFile() {
    return new MockMultipartFile("file", "test.csv", "text/csv", "test content".getBytes());
  }
  private MultipartFile createMockFile_InvalidFileType() {
    return new MockMultipartFile("file", "test.test", "text/csv", "test content".getBytes());
  }

  //Test with invalid headers
  @Test
  void whenInvalidFileType_thenReturnKoResult() {
    MultipartFile file = createMockFile_InvalidFileType();
    ValidationResultDTO validationResultDTO = new ValidationResultDTO("KO","TEST");
    when(productFileValidator.validateFile(any(),anyString(),anyList(),anyInt())).thenReturn(validationResultDTO);
    ProductFileResult res = productFileService.processFile(file, "cat","org","user","email");
    assertEquals("KO", res.getStatus());
    assertEquals("TEST", res.getErrorKey());
  }

  //Test con controlli formali falliti
  private void testFormalError(String errorMessage) {
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

      when(fileStorageClient.upload(any(), any(), any())).thenReturn(null);

      when(productFileValidator.validateFile(any(), any(), any(), anyInt()))
        .thenReturn(new ValidationResultDTO("OK", null));

      CSVRecord invalidRecordLocal = mock(CSVRecord.class);
      List<CSVRecord> invalidRecordsLocal = Collections.singletonList(invalidRecordLocal);
      Map<CSVRecord, String> errorMessagesLocal = new HashMap<>();
      errorMessagesLocal.put(invalidRecordLocal, errorMessage);

      ValidationResultDTO validationResult = new ValidationResultDTO("KO", null, invalidRecordsLocal, errorMessagesLocal);
      when(productFileValidator.validateRecords(any(), any(), any())).thenReturn(validationResult);

      ProductFile savedProductFile = ProductFile.builder().id("123").build();
      when(productFileRepository.save(any())).thenReturn(savedProductFile);

      ProductFileResult result = productFileService.processFile(file, "cookinghobs", "org1", "user1","email");

      assertEquals("KO", result.getStatus());
      assertEquals(AssetRegisterConstants.UploadKeyConstant.REPORT_FORMAL_FILE_ERROR_KEY, result.getErrorKey());
      assertEquals("123", result.getProductFileId());
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

      when(fileStorageClient.upload(any(), any(), any())).thenReturn(null);

      ProductFileResult res = productFileService.processFile(file, "cat", "org", "user","email");

      assertEquals("OK", res.getStatus());
      assertNull(res.getErrorKey());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void shouldThrowExceptionWhenOrganizationIdIsEmpty() {
    ReportNotFoundException ex = assertThrows(ReportNotFoundException.class,
      () -> productFileService.getProductFilesByOrganizationId(""));
    assertEquals("Organization Id is null or empty", ex.getMessage());
  }

  @Test
  void shouldReturnMappedDTOListWhenValidDataIsPresent() {
    Product file = Product.builder()
      .productFileId("file123")
      .category("DISHWASHERS")
      .build();

    when(productRepository.findDistinctProductFileIdAndCategoryByOrganizationId("org123"))
      .thenReturn(List.of(file));

    List<ProductBatchDTO> result = productFileService.getProductFilesByOrganizationId("org123");

    assertEquals(1, result.size());
    assertEquals("file123", result.get(0).getProductFileId());
    assertEquals("DISHWASHERS_file123.csv", result.get(0).getBatchName());
  }

  @Test
  void whenFileAlreadyInProgressOrUploaded_thenReturnKoAlreadyInProgress() {
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "dummy".getBytes());

    // Simula presenza di un file già in stato IN_PROCESS o UPLOADED
    when(productFileRepository.existsByOrganizationIdAndUploadStatusIn(eq("org"), anyList()))
      .thenReturn(true);

    ProductFileResult result = productFileService.processFile(file, "cat", "org", "user", "email");

    assertEquals("KO", result.getStatus());
    assertEquals(AssetRegisterConstants.UploadKeyConstant.UPLOAD_ALREADY_IN_PROGRESS, result.getErrorKey());
  }


}
