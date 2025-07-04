package it.gov.pagopa.register.controller.operation;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.register.dto.operation.FileReportDTO;
import it.gov.pagopa.register.dto.operation.ProductFileDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResult;
import it.gov.pagopa.register.service.operation.ProductFileService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductFileController.class)
class ProductFileControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProductFileService productFileService;

  @Autowired
  private ObjectMapper objectMapper;

  private static final  String TEST_ID_UPLOAD = "example_upload";

  //Test con esito positivo
  @Test
  void testDownloadListUpload_Success() throws Exception {
    ProductFileDTO fileDTO = new ProductFileDTO();
    fileDTO.setFileName("test-file.txt");

    ProductFileResponseDTO mockResponse = ProductFileResponseDTO.builder()
      .content(Collections.singletonList(fileDTO))
      .pageNo(0)
      .pageSize(10)
      .totalElements(1L)
      .totalPages(1)
      .build();

    Mockito.when(productFileService.getFilesByPage(eq("org123"), any(Pageable.class)))
      .thenReturn(mockResponse);

    mockMvc.perform(get("/idpay/register/product-files")
        .header("x-organization-id", "org123")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content").isArray())
      .andExpect(jsonPath("$.pageNo").value(0))
      .andExpect(jsonPath("$.pageSize").value(10))
      .andExpect(jsonPath("$.totalElements").value(1))
      .andExpect(jsonPath("$.totalPages").value(1));

  }

  //Test senza header
  @Test
  void testDownloadListUpload_MissingHeader() throws Exception {
    mockMvc.perform(get("/idpay/register/product-files")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  //Test in caso di eccezione
  @Test
  void testDownloadListUpload_ServiceThrowsException() throws Exception {
    Mockito.when(productFileService.getFilesByPage(eq("org123"), any(Pageable.class)))
      .thenThrow(new RuntimeException("Service error"));

    mockMvc.perform(get("/idpay/register/product-files")
        .header("x-organization-id", "org123")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isInternalServerError());
  }

  @Test
  void downloadCsv_successfulResponse() throws Exception {
    ByteArrayOutputStream file = new ByteArrayOutputStream();
    file.write("fake csv content".getBytes());

    Mockito.when(productFileService.downloadReport(TEST_ID_UPLOAD, "testOrg")).thenReturn(FileReportDTO.builder().data(file.toByteArray()).build());

    mockMvc.perform(get("/idpay/register/product-files/{productFileId}/report", TEST_ID_UPLOAD)
        .param("idProduttore", "testProducer")
        .header("x-organization-id", "testOrg"))
      .andExpect(status().isOk())
      .andExpect(header().string("Content-Disposition", "attachment; filename=test.csv"))
      .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
      .andExpect(content().bytes(file.toByteArray()));
  }

  @Test
  void downloadCsv_notFound() throws Exception {
    Mockito.when(productFileService.downloadReport(TEST_ID_UPLOAD, "testOrg"))
      .thenThrow(new RuntimeException("File not found"));

    mockMvc.perform(get("/idpay/register/product-files/{productFileId}/report", TEST_ID_UPLOAD)
        .header("x-organization-id", "testOrg"))
      .andExpect(status().isInternalServerError());
  }

  @Test
  void uploadProductFile_withInvalidExtension_KoStatus() throws Exception {
    MockMultipartFile wrongFile = new MockMultipartFile(
      "csv", "file.txt", "text/plain", "some,data".getBytes()
    );

    ProductFileResult result = ProductFileResult.ko("EXTENSION_FILE_ERROR");

    Mockito.when(productFileService.processFile(any(), any(), any(), any())).thenReturn(result);

    mockMvc.perform(multipart("/idpay/register/product-files")
        .file(wrongFile)
        .param("category", "eprel")
        .header("x-organization-id", "org-id")
        .header("x-user-id", "user-id"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("KO"))
      .andExpect(jsonPath("$.errorKey").value("EXTENSION_FILE_ERROR"));
  }

  @Test
  void uploadProductFile_withInvalidHeader_KoStatus() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
      "csv", "products.csv", "text/csv", "wrong,header\n1,test".getBytes()
    );

    ProductFileResult result = ProductFileResult.ko("HEADER_FILE_ERROR");

    Mockito.when(productFileService.processFile(any(), any(), any(), any())).thenReturn(result);

    mockMvc.perform(multipart("/idpay/register/product-files")
        .file(file)
        .param("category", "eprel")
        .header("x-organization-id", "org-id")
        .header("x-user-id", "user-id"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("KO"))
      .andExpect(jsonPath("$.errorKey").value("HEADER_FILE_ERROR"));
  }

  @Test
  void uploadProductFile_withTooManyRecords_KoStatus() throws Exception {
    StringBuilder sb = new StringBuilder("id,name\n");

    int maxRows = 5;
    for (int i = 0; i < maxRows+1; i++) {
      sb.append(i).append(",Product ").append(i).append("\n");
    }

    MockMultipartFile file = new MockMultipartFile(
      "csv", "big.csv", "text/csv", sb.toString().getBytes()
    );

    ProductFileResult result = ProductFileResult.ko("MAX_ROW_FILE_ERROR");

    Mockito.when(productFileService.processFile(any(), any(), any(), any())).thenReturn(result);

    mockMvc.perform(multipart("/idpay/register/product-files")
        .file(file)
        .param("category", "eprel")
        .header("x-organization-id", "org-id")
        .header("x-user-id", "user-id"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("KO"))
      .andExpect(jsonPath("$.errorKey").value("MAX_ROW_FILE_ERROR"));
  }

  @Test
  void uploadProductFile_withValidCsv_shouldReturnSuccess() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
      "csv", "valid.csv", "text/csv", "id,name\n1,Product1".getBytes()
    );

    ProductFileResult result = ProductFileResult.ok();

    Mockito.when(productFileService.processFile(any(), any(), any(), any())).thenReturn(result);

    mockMvc.perform(multipart("/idpay/register/product-files")
        .file(file)
        .param("category", "eprel")
        .header("x-organization-id", "org-id")
        .header("x-user-id", "user-id"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("OK"));
  }




}
