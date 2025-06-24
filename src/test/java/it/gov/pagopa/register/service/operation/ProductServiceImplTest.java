package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.model.operation.UploadCsv;
import it.gov.pagopa.register.repository.operation.UploadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {

  @Mock
  private UploadRepository uploadRepository;

  @Mock
  private FileStorageClient azureBlobClient;

  @InjectMocks
  private ProductService productService;

  private final String ID_UPLOAD_CORRECT = "example_eprel";

  //Test con errori Eprel
  @Test
  void downloadReport_withEprelKo() {
    UploadCsv uploadCsv = new UploadCsv();
    uploadCsv.setIdUpload(ID_UPLOAD_CORRECT);
    uploadCsv.setStatus("EPREL_KO");

    ByteArrayOutputStream expectedStream = new ByteArrayOutputStream();

    when(uploadRepository.findByIdUpload(ID_UPLOAD_CORRECT)).thenReturn(Optional.of(uploadCsv));
    when(azureBlobClient.download("Report/Eprel_Error/" + ID_UPLOAD_CORRECT + ".csv")).thenReturn(expectedStream);

    ByteArrayOutputStream result = productService.downloadReport(ID_UPLOAD_CORRECT);

    assertEquals(expectedStream, result);
  }

  //Test con errori formali
  @Test
  void downloadReport_withFormalKo() {
    UploadCsv upload = new UploadCsv();
    upload.setIdUpload(ID_UPLOAD_CORRECT);
    upload.setStatus("FORMAL_KO");

    ByteArrayOutputStream expectedStream = new ByteArrayOutputStream();

    when(uploadRepository.findByIdUpload(ID_UPLOAD_CORRECT)).thenReturn(Optional.of(upload));
    when(azureBlobClient.download("Report/Formal_Error/" + ID_UPLOAD_CORRECT + ".csv")).thenReturn(expectedStream);

    ByteArrayOutputStream result = productService.downloadReport(ID_UPLOAD_CORRECT);

    assertEquals(expectedStream, result);
  }

  //Test con idUpload errato -> ritorna un'eccezione
  @Test
  void downloadReport_withInvalidId() {
    when(uploadRepository.findByIdUpload(ID_UPLOAD_CORRECT)).thenReturn(Optional.empty());

    ReportNotFoundException ex = assertThrows(
      ReportNotFoundException.class,
      () -> productService.downloadReport(ID_UPLOAD_CORRECT)
    );

    assertTrue(ex.getMessage().contains("Report non trovato con id"));
  }

  //Test con status errato -> ritorna un'eccezione
  @Test
  void downloadReport_withUnsupportedStatus() {
    UploadCsv upload = new UploadCsv();
    upload.setIdUpload(ID_UPLOAD_CORRECT);
    upload.setStatus("UNKNOWN");

    when(uploadRepository.findByIdUpload(ID_UPLOAD_CORRECT)).thenReturn(Optional.of(upload));

    ReportNotFoundException ex = assertThrows(
      ReportNotFoundException.class,
      () -> productService.downloadReport(ID_UPLOAD_CORRECT)
    );

    assertTrue(ex.getMessage().contains("Tipo di errore non supportato"));
  }

  //Test quando Azure fallisce -> ritorna un'eccezione
  @Test
  void downloadReport_whenAzureReturnsNull() {
    UploadCsv upload = new UploadCsv();
    upload.setIdUpload(ID_UPLOAD_CORRECT);
    upload.setStatus("FORMAL_KO");

    when(uploadRepository.findByIdUpload(ID_UPLOAD_CORRECT)).thenReturn(Optional.of(upload));
    when(azureBlobClient.download("Report/Formal_Error/" + ID_UPLOAD_CORRECT + ".csv")).thenReturn(null);

    ReportNotFoundException ex = assertThrows(
      ReportNotFoundException.class,
      () -> productService.downloadReport(ID_UPLOAD_CORRECT)
    );

    assertTrue(ex.getMessage().contains("Report non trovato su Azure"));
  }
}
