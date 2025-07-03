package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import org.junit.jupiter.api.BeforeEach;
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
class ProductUploadCSVServiceTest {

  @Mock
  private ProductFileRepository uploadRepository;

  @Mock
  private FileStorageClient azureBlobClient;

  @InjectMocks
  private ProductFileService productUploadCSVService;

  private final String id = "example_id";
  private final String orgId = "example_orgId";

  @BeforeEach
  void setUp() {
    //productUploadCSVService.maxRows = 5; // Imposta maxRows per il test
  }

  //-------------------------Test su metodo upload csv--------------------



  //-------------------------Test su metodo download report--------------------

  //Test con errori Eprel
  @Test
  void downloadReport_withEprelKo() {
    ProductFile uploadCsv = new ProductFile();
    uploadCsv.setId(id);
    uploadCsv.setOrganizationId(orgId);
    uploadCsv.setUploadStatus("EPREL_ERROR");


    ByteArrayOutputStream expectedStream = new ByteArrayOutputStream();

    when(uploadRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(uploadCsv));
    when(azureBlobClient.download("Report/Eprel_Error/" + id + ".csv")).thenReturn(expectedStream);

    ByteArrayOutputStream result = productUploadCSVService.downloadReport(id, orgId);

    assertEquals(expectedStream, result);
  }

  //Test con errori formali
  @Test
  void downloadReport_withFormalKo() {
    ProductFile uploadCsv = new ProductFile();
    uploadCsv.setId(id);
    uploadCsv.setOrganizationId(orgId);
    uploadCsv.setUploadStatus("FORMAL_ERROR");

    ByteArrayOutputStream expectedStream = new ByteArrayOutputStream();

    when(uploadRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(uploadCsv));
    when(azureBlobClient.download("Report/Formal_Error/" + id + ".csv")).thenReturn(expectedStream);

    ByteArrayOutputStream result = productUploadCSVService.downloadReport(id, orgId);

    assertEquals(expectedStream, result);
  }

  //Test con idUpload errato -> ritorna un'eccezione
  @Test
  void downloadReport_withInvalidId() {
    when(uploadRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.empty());

    ReportNotFoundException ex = assertThrows(
      ReportNotFoundException.class,
      () -> productUploadCSVService.downloadReport(id, orgId)
    );

    assertTrue(ex.getMessage().contains("Report not found with id: " + id));
  }

  //Test con status errato -> ritorna un'eccezione
  @Test
  void downloadReport_withUnsupportedStatus() {
    ProductFile uploadCsv = new ProductFile();
    uploadCsv.setId(id);
    uploadCsv.setOrganizationId(orgId);
    uploadCsv.setUploadStatus("UNKNOWN");
    uploadCsv.setFileName("UNKNOWN");


    when(uploadRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(uploadCsv));

    ReportNotFoundException ex = assertThrows(
      ReportNotFoundException.class,
      () -> productUploadCSVService.downloadReport(id, orgId)
    );

    assertTrue(ex.getMessage().contains("Report not available for file: UNKNOWN"));
  }

  //Test quando Azure fallisce -> ritorna un'eccezione
  @Test
  void downloadReport_whenAzureReturnsNull() {
    ProductFile uploadCsv = new ProductFile();
    uploadCsv.setId(id);
    uploadCsv.setOrganizationId(orgId);
    uploadCsv.setUploadStatus("FORMAL_ERROR");

    when(uploadRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(uploadCsv));
    when(azureBlobClient.download("Report/Formal_Error/" + id + ".csv")).thenReturn(null);

    ReportNotFoundException ex = assertThrows(
      ReportNotFoundException.class,
      () -> productUploadCSVService.downloadReport(id, orgId)
    );

    assertTrue(ex.getMessage().contains("Report not found on Azure for path: Report/Formal_Error/" + id + ".csv"));
  }
}
