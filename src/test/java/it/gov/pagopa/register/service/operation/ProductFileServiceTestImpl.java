package it.gov.pagopa.register.service.operation;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

class ProductFileServiceTest {

  @Mock
  private ProductFileRepository productFileRepository;

  @InjectMocks
  private ProductFileService productFileService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  //Test con esito positivo dell'api
  @Test
  void testDownloadFilesByPage_Success() {
    String organizationId = "org123";
    Pageable pageable = PageRequest.of(0, 2);

    ProductFile file1 = new ProductFile("file1");
    ProductFile file2 = new ProductFile("file2");

    List<ProductFile> fileList = Arrays.asList(file1, file2);
    Page<ProductFile> filesPage = new PageImpl<>(fileList, pageable, fileList.size());

    when(productFileRepository.findByOrganizationIdAndUploadStatusNot(organizationId, "FORMAL_ERROR", pageable))
      .thenReturn(filesPage);

    ProductFileResponseDTO response = productFileService.getFilesByPage(organizationId, pageable);

    assertEquals(2, response.getContent().size());
    assertEquals(0, response.getPageNo());
    assertEquals(2, response.getPageSize());
    assertEquals(2, response.getTotalElements());
    assertEquals(1, response.getTotalPages());

    verify(productFileRepository, times(1))
      .findByOrganizationIdAndUploadStatusNot(organizationId, "FORMAL_ERROR", pageable);
  }

  //Test con nessun risultato dell'api
  @Test
  void testDownloadFilesByPage_EmptyPage() {
    String organizationId = "org123";
    Pageable pageable = PageRequest.of(0, 2);

    List<ProductFile> fileList = Collections.emptyList();
    Page<ProductFile> filesPage = new PageImpl<>(fileList, pageable, 0);

    when(productFileRepository.findByOrganizationIdAndUploadStatusNot(organizationId, "FORMAL_ERROR", pageable))
      .thenReturn(filesPage);

    ProductFileResponseDTO response = productFileService.getFilesByPage(organizationId, pageable);

    assertEquals(0, response.getContent().size());
    assertEquals(0, response.getPageNo());
    assertEquals(2, response.getPageSize());
    assertEquals(0, response.getTotalElements());
    assertEquals(0, response.getTotalPages());

    verify(productFileRepository, times(1))
      .findByOrganizationIdAndUploadStatusNot(organizationId, "FORMAL_ERROR", pageable);
  }

  //Test con eccezioni da parte del repository
  @Test
  void testDownloadFilesByPage_RepositoryThrowsException() {
    String organizationId = "org123";
    Pageable pageable = PageRequest.of(0, 2);

    when(productFileRepository.findByOrganizationIdAndUploadStatusNot(organizationId, "FORMAL_ERROR", pageable))
      .thenThrow(new RuntimeException("Database error"));

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      productFileService.getFilesByPage(organizationId, pageable);
    });

    assertEquals("Database error", exception.getMessage());

    verify(productFileRepository, times(1))
      .findByOrganizationIdAndUploadStatusNot(organizationId, "FORMAL_ERROR", pageable);
  }

  //Test con mancato organizationId
  @Test
  void testDownloadFilesByPage_NullOrganizationId() {
    Pageable pageable = PageRequest.of(0, 2);

    // In base alla tua implementazione, se non c'è un controllo su null, lancerà una NullPointerException dal repository
    // Se invece gestisci l'input a monte, dovresti eventualmente lanciare una IllegalArgumentException personalizzata.

    assertThrows(IllegalArgumentException.class, () -> {
      productFileService.getFilesByPage(null, pageable);
    });
  }

}
