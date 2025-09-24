package it.gov.pagopa.register.service.clean;

import com.azure.core.http.rest.Response;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CleanServiceTest {

  @InjectMocks
  private CleanService cleanService;
  @Mock
  private ProductRepository productRepository;
  @Mock
  private ProductFileRepository productFileRepository;
  @Mock
  private FileStorageClient fileStorageClient;

  private final List<String> ids = List.of("TEST");
  @Test
  void testRemoveProducts()  {
    cleanService.removeProducts(ids);
    verify(productRepository).deleteAllById(ids);
  }

  @Test
  void testRemoveProductsFile() {
    cleanService.removeProductsFile(ids);
    verify(productFileRepository).deleteAllById(ids);
  }

  @Test
  void testRemoveReportFileFromStorage() {
    Response<Boolean> mockResponse = mock(Response.class);
    when(fileStorageClient.deleteFile(any())).thenReturn(mockResponse);

    cleanService.removeReportFileFromStorage(ids);
    verify(fileStorageClient, times(ids.size())).deleteFile(any());
  }

  @Test
  void testRemoveFormalFileFromStorage() {
    Response<Boolean> mockResponse = mock(Response.class);
    when(fileStorageClient.deleteFile(any())).thenReturn(mockResponse);

    cleanService.removeFormalFileFromStorage(ids);
    verify(fileStorageClient, times(ids.size())).deleteFile(any());
  }




}
