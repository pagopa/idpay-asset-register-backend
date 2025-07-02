package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.exception.operation.CsvValidationException;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

  @Mock
  private ProductFileRepository productFileRepository;

  @Mock
  private FileStorageClient azureBlobClient;

  @InjectMocks
  private ProductService productService;

  private final String ID_UPLOAD_CORRECT = "example_eprel";

  @BeforeEach
  void setUp() {
    productService.maxRows = 5; // Imposta maxRows per il test
  }

  //-------------------------Test su metodo upload csv--------------------

  //File di estensione diversa da .csv
  @Test
  void shouldThrowExceptionWhenFileIsNotCsv() {
    MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "header\nvalue".getBytes());

    assertThrows(CsvValidationException.class, () ->
      productService.saveCsv(file, "category", "orgId", "userId")
    );

    verifyNoInteractions(productFileRepository, azureBlobClient);
  }


  //-------------------------Test su metodo download report--------------------

  //Test con errori Eprel
  @Test
  void downloadReport_withEprelKo() {
    ProductFile upload = new ProductFile();
    upload.setId(ID_UPLOAD_CORRECT);
    upload.setUploadStatus("EPREL_ERROR");

    ByteArrayOutputStream expectedStream = new ByteArrayOutputStream();

    when(productFileRepository.findById(ID_UPLOAD_CORRECT)).thenReturn(Optional.of(upload));
    when(azureBlobClient.download("Report/Eprel_Error/" + ID_UPLOAD_CORRECT + ".csv")).thenReturn(expectedStream);

    ByteArrayOutputStream result = productService.downloadReport(ID_UPLOAD_CORRECT);

    assertEquals(expectedStream, result);
  }

  //Test con errori formali
  @Test
  void downloadReport_withFormalKo() {
    ProductFile upload = new ProductFile();
    upload.setId(ID_UPLOAD_CORRECT);
    upload.setUploadStatus("FORMAL_ERROR");


    ByteArrayOutputStream expectedStream = new ByteArrayOutputStream();

    when(productFileRepository.findById(ID_UPLOAD_CORRECT)).thenReturn(Optional.of(upload));
    when(azureBlobClient.download("Report/Formal_Error/" + ID_UPLOAD_CORRECT + ".csv")).thenReturn(expectedStream);

    ByteArrayOutputStream result = productService.downloadReport(ID_UPLOAD_CORRECT);

    assertEquals(expectedStream, result);
  }

  //Test con idUpload errato -> ritorna un'eccezione
  @Test
  void downloadReport_withInvalidId() {
    when(productFileRepository.findById(ID_UPLOAD_CORRECT)).thenReturn(Optional.empty());

    ReportNotFoundException ex = assertThrows(
      ReportNotFoundException.class,
      () -> productService.downloadReport(ID_UPLOAD_CORRECT)
    );

    assertTrue(ex.getMessage().contains("Report non trovato con id"));
  }

  //Test con status errato -> ritorna un'eccezione
  @Test
  void downloadReport_withUnsupportedStatus() {
    ProductFile upload = new ProductFile();
    upload.setId(ID_UPLOAD_CORRECT);
    upload.setUploadStatus("UNKNOWN");


    when(productFileRepository.findById(ID_UPLOAD_CORRECT)).thenReturn(Optional.of(upload));

    ReportNotFoundException ex = assertThrows(
      ReportNotFoundException.class,
      () -> productService.downloadReport(ID_UPLOAD_CORRECT)
    );

    assertTrue(ex.getMessage().contains("Tipo di errore non supportato"));
  }

  //Test quando Azure fallisce -> ritorna un'eccezione
  @Test
  void downloadReport_whenAzureReturnsNull() {
    ProductFile upload = new ProductFile();
    upload.setId(ID_UPLOAD_CORRECT);
    upload.setUploadStatus("FORMAL_ERROR");


    when(productFileRepository.findById(ID_UPLOAD_CORRECT)).thenReturn(Optional.of(upload));
    when(azureBlobClient.download("Report/Formal_Error/" + ID_UPLOAD_CORRECT + ".csv")).thenReturn(null);

    ReportNotFoundException ex = assertThrows(
      ReportNotFoundException.class,
      () -> productService.downloadReport(ID_UPLOAD_CORRECT)
    );

    assertTrue(ex.getMessage().contains("Report non trovato su Azure"));
  }
}
