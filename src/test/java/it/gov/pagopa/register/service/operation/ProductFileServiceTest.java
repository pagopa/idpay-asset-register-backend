package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.enums.UploadCsvStatus;
import it.gov.pagopa.register.dto.operation.*;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.service.validator.file.ProductFileValidatorService;
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

import static it.gov.pagopa.register.constants.AssetRegisterConstants.UploadError.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductFileServiceTest {

  @Mock
  private ProductFileRepository productFileRepository;
  @Mock
  private ProductRepository productRepository;
  @Mock
  private FileStorageClient fileStorageClient;
  @Mock
  private ProductFileValidatorService productFileValidator;
  @Mock
  private ProducersInitiativeRepository producersInitiativeRepository;

  private ProductFileService productFileService;

  private static final String INITIATIVE_ID = "ini1";
  private static final String ORG_ID = "org1";
  private static final String INITIATIVE_KEY = ORG_ID + "_" + INITIATIVE_ID;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    productFileService = new ProductFileService(productFileRepository, productRepository, fileStorageClient, productFileValidator, producersInitiativeRepository);
  }

  @Test
  void testGetFilesByPage_Success() {
    Pageable page = PageRequest.of(0, 2);
    ProductFile pf1 = ProductFile.builder().id("1").fileName("f1.csv").uploadStatus("OK").category("WASHINGMACHINES").build();
    ProductFile pf2 = ProductFile.builder().id("2").fileName("f2.csv").uploadStatus("OK").category("WASHINGMACHINES").build();
    List<ProductFile> list = List.of(pf1, pf2);
    Page<ProductFile> pg = new PageImpl<>(list, page, list.size());
    when(productFileRepository.findByOrganizationIdAndInitiativeIdAndUploadStatusNot(ORG_ID, INITIATIVE_ID, UploadCsvStatus.FORMAL_ERROR.name(), page))
      .thenReturn(pg);

    ProductFileResponseDTO resp = productFileService.getFilesByPage(ORG_ID, INITIATIVE_ID, page);
    assertEquals(2, resp.getContent().size());
    assertEquals("f1.csv", resp.getContent().get(0).getFileName());
    assertEquals(0, resp.getPageNo());
    assertEquals(2, resp.getPageSize());
    assertEquals(2, resp.getTotalElements());
    assertEquals(1, resp.getTotalPages());
  }

  @Test
  void testGetFilesByPage_Empty() {
    Pageable page = PageRequest.of(0, 2);
    Page<ProductFile> pg = new PageImpl<>(List.of(), page, 0);
    when(productFileRepository.findByOrganizationIdAndInitiativeIdAndUploadStatusNot(ORG_ID, INITIATIVE_ID, UploadCsvStatus.FORMAL_ERROR.name(), page))
      .thenReturn(pg);

    ProductFileResponseDTO resp = productFileService.getFilesByPage(ORG_ID, INITIATIVE_ID, page);
    assertTrue(resp.getContent().isEmpty());
    assertEquals(0, resp.getTotalElements());
  }

  @Test
  void testGetFilesByPage_RepoThrows() {
    Pageable page = PageRequest.of(0, 1);
    when(productFileRepository.findByOrganizationIdAndInitiativeIdAndUploadStatusNot(any(), any(), any(), eq(page)))
      .thenThrow(new RuntimeException("DB"));
    assertThrows(RuntimeException.class, () -> productFileService.getFilesByPage(ORG_ID, INITIATIVE_ID, page));
  }

  @Test
  void downloadReport_partialLoad() throws IOException {
    ProductFile productFile = ProductFile.builder()
      .id("1")
      .organizationId(ORG_ID)
      .initiativeId(INITIATIVE_ID)
      .uploadStatus("PARTIAL")
      .fileName("eprel_report.csv")
      .build();

    ByteArrayOutputStream mockedOutput = new ByteArrayOutputStream();
    mockedOutput.write("dummy report content".getBytes());

    when(productFileRepository.findByIdAndOrganizationIdAndInitiativeId("1", ORG_ID, INITIATIVE_ID))
      .thenReturn(Optional.of(productFile));

    String expectedPath = AssetRegisterConstants.REPORT_PARTIAL_ERROR + INITIATIVE_ID + "/1.csv";
    when(fileStorageClient.download(expectedPath)).thenReturn(mockedOutput);

    FileReportDTO reportDTO = productFileService.downloadReport("1", ORG_ID, INITIATIVE_ID);

    assertNotNull(reportDTO);
    assertArrayEquals("dummy report content".getBytes(), reportDTO.getData());
    assertEquals("eprel_report_errors.csv", reportDTO.getFilename());
  }

  @Test
  void downloadReport_unsupportedStatus() {
    ProductFile pf = ProductFile.builder()
      .id("1")
      .organizationId(ORG_ID)
      .initiativeId(INITIATIVE_ID)
      .uploadStatus("UNKNOWN")
      .fileName("f.csv")
      .build();

    when(productFileRepository.findByIdAndOrganizationIdAndInitiativeId("1", ORG_ID, INITIATIVE_ID))
      .thenReturn(Optional.of(pf));

    assertThrows(ReportNotFoundException.class, () -> productFileService.downloadReport("1", ORG_ID, INITIATIVE_ID));
  }

  @Test
  void downloadReport_azureNull() {
    ProductFile pf = ProductFile.builder()
      .id("1")
      .organizationId(ORG_ID)
      .initiativeId(INITIATIVE_ID)
      .uploadStatus("FORMAL_ERROR")
      .build();

    when(productFileRepository.findByIdAndOrganizationIdAndInitiativeId("1", ORG_ID, INITIATIVE_ID))
      .thenReturn(Optional.of(pf));

    String expectedPath = AssetRegisterConstants.REPORT_FORMAL_ERROR + INITIATIVE_ID + "/1.csv";
    when(fileStorageClient.download(expectedPath)).thenReturn(null);

    ReportNotFoundException ex = assertThrows(ReportNotFoundException.class, () -> productFileService.downloadReport("1", ORG_ID, INITIATIVE_ID));
    assertEquals("File not found on storage", ex.getMessage());
  }

  @Test
  void downloadReport_notFound_ShouldLogAndThrowException() {
    when(productFileRepository.findByIdAndOrganizationIdAndInitiativeId("1", ORG_ID, INITIATIVE_ID))
      .thenReturn(Optional.empty());

    assertThrows(ReportNotFoundException.class, () -> productFileService.downloadReport("1", ORG_ID, INITIATIVE_ID));
    verifyNoInteractions(fileStorageClient);
  }

  @Test
  void whenInvalidFileType_thenReturnKoResult() throws IOException {
    MultipartFile file = new MockMultipartFile("csv", "test.test", "text/csv", "content".getBytes());
    ValidationResultDTO validationResultDTO = new ValidationResultDTO("KO", "TEST");

    ProducersInitiative producersInitiative = new ProducersInitiative();
    producersInitiative.setEnabled(true);
    producersInitiative.setProducerEmail("test@pagopa.it");
    when(producersInitiativeRepository.findById(INITIATIVE_KEY)).thenReturn(Optional.of(producersInitiative));

    when(productFileValidator.validateFile(any(), any(), any(), any())).thenReturn(validationResultDTO);

    ProductFileResult res = productFileService.uploadFile(file, "cat", INITIATIVE_ID, ORG_ID, "user", "orgName");

    assertEquals("KO", res.getStatus());
    assertEquals("TEST", res.getErrorKey());
  }

  private void testFormalError(String errorMessage) {
    MultipartFile file = new MockMultipartFile("csv", "test.csv", "text/csv", "content".getBytes());

    try (MockedStatic<CsvUtils> mockedCsv = mockStatic(CsvUtils.class);
         MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {

      mockedCsv.when(() -> CsvUtils.writeCsvWithErrors(any(), any(), any(), any()))
        .thenAnswer(inv -> null);

      mockedFiles.when(() -> Files.newInputStream(any()))
        .thenReturn(new ByteArrayInputStream("dummy".getBytes()));

      ProducersInitiative producersInitiative = new ProducersInitiative();
      producersInitiative.setEnabled(true);
      producersInitiative.setProducerEmail("test@pagopa.it");
      when(producersInitiativeRepository.findById(INITIATIVE_KEY)).thenReturn(Optional.of(producersInitiative));

      when(productFileValidator.validateFile(any(), any(), any(), any()))
        .thenReturn(new ValidationResultDTO("OK", null, List.of(mock(CSVRecord.class)), List.of("H1")));

      CSVRecord invalidRecordLocal = mock(CSVRecord.class);
      ValidationResultDTO validationResult = new ValidationResultDTO("KO", null, List.of(invalidRecordLocal), Map.of(invalidRecordLocal, errorMessage));
      when(productFileValidator.validateRecords(any(), any(), any())).thenReturn(validationResult);

      ProductFile savedProductFile = ProductFile.builder().id("123").initiativeId(INITIATIVE_ID).build();
      when(productFileRepository.save(any())).thenReturn(savedProductFile);

      ProductFileResult result = productFileService.uploadFile(file, "COOKINGHOBS", INITIATIVE_ID, ORG_ID, "user", "orgName");

      assertEquals("KO", result.getStatus());
      assertEquals(REPORT_FORMAL_FILE_ERROR_KEY, result.getErrorKey());
      assertEquals("123", result.getProductFileId());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void whenInvalidGtin_thenReturnFormalError() {
    testFormalError("Il Codice GTIN/EAN è obbligatorio");
  }

  @Test
  void whenInvalidProductCode_thenReturnFormalError() {
    testFormalError("Il Codice prodotto non è valido");
  }

  @Test
  void whenInvalidCategory_thenReturnFormalError() {
    testFormalError("Il campo Categoria è obbligatorio");
  }

  @Test
  void whenInvalidCountry_thenReturnFormalError() {
    testFormalError("Il Paese di Produzione non è valido");
  }

  @Test
  void whenInvalidBrand_thenReturnFormalError() {
    testFormalError("Il campo Marca è obbligatorio");
  }

  @Test
  void whenInvalidModel_thenReturnFormalError() {
    testFormalError("Il campo Modello è obbligatorio");
  }

  @Test
  void whenAllValid_thenReturnOk() throws IOException {
    MultipartFile file = mock(MultipartFile.class);
    CSVRecord rec = mock(CSVRecord.class);

    ProducersInitiative producersInitiative = new ProducersInitiative();
    producersInitiative.setEnabled(true);
    producersInitiative.setProducerEmail("test@pagopa.it");
    when(producersInitiativeRepository.findById(INITIATIVE_KEY)).thenReturn(Optional.of(producersInitiative));

    when(productFileValidator.validateFile(any(), any(), any(), any()))
      .thenReturn(ValidationResultDTO.ok(List.of(rec), List.of("C1")));

    when(productFileValidator.validateRecords(anyList(), anyString(), anyString()))
      .thenReturn(ValidationResultDTO.ok());

    when(productFileRepository.save(any())).thenReturn(ProductFile.builder().id("42").build());

    when(file.getInputStream()).thenReturn(new ByteArrayInputStream("abc".getBytes()));
    when(file.getOriginalFilename()).thenReturn("f.csv");
    when(file.getContentType()).thenReturn("text/csv");

    ProductFileResult res = productFileService.uploadFile(file, "cat", INITIATIVE_ID, ORG_ID, "user", "orgName");

    assertEquals("OK", res.getStatus());
    assertNull(res.getErrorKey());
    verify(fileStorageClient).upload(any(), anyString(), any());
  }

  @Test
  void shouldReturnMappedDTOListWhenValidDataIsPresent() {
    Product file = Product.builder()
      .productFileId("file123")
      .category("DISHWASHERS")
      .build();

    when(productRepository.retrieveDistinctProductFileIdsBasedOnRole(ORG_ID, INITIATIVE_ID, null, "operatore"))
      .thenReturn(List.of(file));

    List<ProductBatchDTO> result = productFileService.retrieveDistinctProductFileIdsBasedOnRole(ORG_ID, INITIATIVE_ID, null, "operatore");

    assertEquals(1, result.size());
    assertEquals("file123", result.getFirst().getProductFileId());
    assertEquals("Lavastoviglie_file123.csv", result.getFirst().getBatchName());
  }

  @Test
  void whenFileAlreadyInProgressOrUploaded_thenReturnKoAlreadyInProgress() {
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "dummy".getBytes());

    ProducersInitiative producersInitiative = new ProducersInitiative();
    producersInitiative.setEnabled(true);
    producersInitiative.setProducerEmail("test@pagopa.it");
    when(producersInitiativeRepository.findById(INITIATIVE_KEY)).thenReturn(Optional.of(producersInitiative));

    when(productFileRepository.existsByInitiativeIdAndOrganizationIdAndUploadStatusIn(eq(INITIATIVE_ID), eq(ORG_ID), any()))
      .thenReturn(true);

    ProductFileResult result = productFileService.uploadFile(file, "cat", INITIATIVE_ID, ORG_ID, "user", "orgName");

    assertEquals("KO", result.getStatus());
    assertEquals(UPLOAD_ALREADY_IN_PROGRESS, result.getErrorKey());
  }

  @Test
  void whenOrganizationNotEnabled_thenReturnKoPermission() {
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "dummy".getBytes());

    ProducersInitiative producersInitiative = new ProducersInitiative();
    producersInitiative.setEnabled(false);
    when(producersInitiativeRepository.findById(INITIATIVE_KEY)).thenReturn(Optional.of(producersInitiative));

    ProductFileResult result = productFileService.uploadFile(file, "cat", INITIATIVE_ID, ORG_ID, "user", "orgName");

    assertEquals("KO", result.getStatus());
    assertEquals(NOT_ENABLED_ERRORE_KEY, result.getErrorKey());
  }

  @Test
  void whenGenericExceptionThrown_thenReturnKoGenericError() throws IOException {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn("test.csv");

    ProducersInitiative producersInitiative = new ProducersInitiative();
    producersInitiative.setEnabled(true);
    producersInitiative.setProducerEmail("test@pagopa.it");
    when(producersInitiativeRepository.findById(INITIATIVE_KEY)).thenReturn(Optional.of(producersInitiative));

    when(productFileValidator.validateFile(any(), any(), any(), any()))
      .thenThrow(new RuntimeException("Simulated unexpected error"));

    ProductFileResult result = productFileService.uploadFile(file, "cat", INITIATIVE_ID, ORG_ID, "user", "orgName");

    assertEquals("KO", result.getStatus());
    assertEquals("GENERIC_ERROR", result.getErrorKey());
  }

  @Test
  void whenInitiativeNotFound_thenReturnKoPermission() {
    MultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "dummy".getBytes());

    when(producersInitiativeRepository.findById(INITIATIVE_KEY)).thenReturn(Optional.empty());

    ProductFileResult result = productFileService.validateFile(file, "cat", INITIATIVE_ID, ORG_ID, "user", "orgName");

    assertEquals("KO", result.getStatus());
    assertEquals(NOT_ENABLED_ERRORE_KEY, result.getErrorKey());
  }

}
