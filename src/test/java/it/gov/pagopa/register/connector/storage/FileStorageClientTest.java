package it.gov.pagopa.register.connector.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import it.gov.pagopa.register.configuration.FileStorageConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {
  FileStorageClient.class,
  FileStorageConfig.class
})
@TestPropertySource(properties = {
  "azure.storage.connection-string=your-connection-string",
  "azure.storage.container-name=your-container-reference"
})
@SpringBootTest
class FileStorageClientTest {

  @MockitoBean
  private BlobContainerClient blobContainerClient;

  @Autowired
  private FileStorageClient fileStorageClient;

  @Mock
  private BlobClient blobClient;

  @Test
  void uploadFile_ShouldUploadSuccessfully() {
    // Given
    File file = new File("test.txt");
    String destination = "folder/test.txt";
    String contentType = "text/plain";

    Response<BlockBlobItem> mockResponse = mock(Response.class);

    when(blobContainerClient.getBlobClient(destination)).thenReturn(blobClient);
    when(blobClient.uploadFromFileWithResponse(any(BlobUploadFromFileOptions.class), any(), any()))
      .thenReturn(mockResponse);

    // When
    Response<BlockBlobItem> result = fileStorageClient.uploadFile(file, destination, contentType);

    // Then
    assertThat(result).isEqualTo(mockResponse);
    verify(blobContainerClient).getBlobClient(destination);
    verify(blobClient).uploadFromFileWithResponse(any(BlobUploadFromFileOptions.class), any(), any());
  }

  @Test
  void upload_ShouldUploadFromInputStream() {
    // Given
    InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
    String destination = "folder/test.txt";
    String contentType = "text/plain";

    Response<BlockBlobItem> mockResponse = mock(Response.class);

    when(blobContainerClient.getBlobClient(destination)).thenReturn(blobClient);
    when(blobClient.uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any()))
      .thenReturn(mockResponse);

    // When
    Response<BlockBlobItem> result = fileStorageClient.upload(inputStream, destination, contentType);

    // Then
    assertThat(result).isEqualTo(mockResponse);
    verify(blobContainerClient).getBlobClient(destination);
    verify(blobClient).uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any());
  }

  @Test
  void deleteFile_ShouldDeleteSuccessfully() {
    // Given
    String destination = "folder/test.txt";
    Response<Boolean> mockResponse = mock(Response.class);

    when(blobContainerClient.getBlobClient(destination)).thenReturn(blobClient);
    when(blobClient.deleteIfExistsWithResponse(any(), any(), any(), any()))
      .thenReturn(mockResponse);

    // When
    Response<Boolean> result = fileStorageClient.deleteFile(destination);

    // Then
    assertThat(result).isEqualTo(mockResponse);
    verify(blobContainerClient).getBlobClient(destination);
    verify(blobClient).deleteIfExistsWithResponse(any(), any(), any(), any());
  }

  @Test
  void download_ShouldReturnNullWhen404() {
    // Given
    String filePath = "nonexistent.txt";
    BlobStorageException exception = mock(BlobStorageException.class);
    when(exception.getStatusCode()).thenReturn(404);

    when(blobContainerClient.getBlobClient(filePath)).thenReturn(blobClient);
    doThrow(exception).when(blobClient).downloadStream(any(OutputStream.class));

    // When
    ByteArrayOutputStream result = fileStorageClient.download(filePath);

    // Then
    assertThat(result).isNull();
    verify(blobContainerClient).getBlobClient(filePath);
    verify(blobClient).downloadStream(any(OutputStream.class));
  }

  @Test
  void download_ShouldThrowExceptionWhenServerError() {
    // Given
    String filePath = "test.txt";
    BlobStorageException exception = mock(BlobStorageException.class);
    when(exception.getStatusCode()).thenReturn(500);

    when(blobContainerClient.getBlobClient(filePath)).thenReturn(blobClient);
    doThrow(exception).when(blobClient).downloadStream(any(OutputStream.class));

    // When & Then
    assertThatThrownBy(() -> fileStorageClient.download(filePath))
      .isInstanceOf(BlobStorageException.class);
  }

  @Test
  void downloadToFile_ShouldDownloadSuccessfully() throws IOException {
    // Given
    String filePath = "test.txt";
    Path destination = Files.createTempFile("test", ".txt");

    Response<BlobProperties> mockResponse = mock(Response.class);

    when(blobContainerClient.getBlobClient(filePath)).thenReturn(blobClient);
    when(blobClient.downloadToFileWithResponse(any(BlobDownloadToFileOptions.class), any(), any()))
      .thenReturn(mockResponse);

    try {
      // When
      Response<BlobProperties> result = fileStorageClient.download(filePath, destination);

      // Then
      assertThat(result).isEqualTo(mockResponse);
      verify(blobContainerClient).getBlobClient(filePath);
      verify(blobClient).downloadToFileWithResponse(any(BlobDownloadToFileOptions.class), any(), any());
    } finally {
      // Cleanup
      Files.deleteIfExists(destination);
    }
  }

  @Test
  void listFiles_ShouldReturnPagedIterable() {
    // Given
    String path = "folder/";
    PagedIterable<BlobItem> mockPagedIterable = mock(PagedIterable.class);

    when(blobContainerClient.listBlobsByHierarchy(path)).thenReturn(mockPagedIterable);

    // When
    PagedIterable<BlobItem> result = fileStorageClient.listFiles(path);

    // Then
    assertThat(result).isEqualTo(mockPagedIterable);
    verify(blobContainerClient).listBlobsByHierarchy(path);
  }
}
