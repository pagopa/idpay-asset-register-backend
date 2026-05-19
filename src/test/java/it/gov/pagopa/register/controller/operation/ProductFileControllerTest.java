package it.gov.pagopa.register.controller.operation;


import it.gov.pagopa.register.dto.operation.*;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.service.operation.ProductFileService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value={ProductFileController.class}, excludeAutoConfiguration =  { UserDetailsServiceAutoConfiguration.class , SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class ProductFileControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProductFileService productFileService;

  @Autowired
  private ObjectMapper objectMapper;

  private static final  String TEST_ID_UPLOAD = "687f8a176a5c92458819922a";
  private static final String INITIATIVE_ID = "687f8a176a5c92458819922b";

  @Test
  void testDownloadListUpload_Success() throws Exception {
    String initiativeId = INITIATIVE_ID;
    ProductFileDTO fileDTO = new ProductFileDTO();
    fileDTO.setFileName("test-file.txt");

    ProductFileResponseDTO mockResponse = ProductFileResponseDTO.builder()
      .content(Collections.singletonList(fileDTO))
      .build();

    Mockito.when(productFileService.getFilesByPage(eq("83843864-f3c0-4def-badb-7f197471b72e"), any(), any(Pageable.class)))
      .thenReturn(mockResponse);

    mockMvc.perform(get("/idpay/register/initiatives/{initiativeId}/product-files", initiativeId)
        .header("x-organization-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].fileName").value("test-file.txt"));
  }


  @Test
  void testDownloadListUpload_MissingHeader() throws Exception {
    mockMvc.perform(get("/idpay/register/initiatives/{initiativeId}/product-files", INITIATIVE_ID)
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void downloadCsv_successfulResponse() throws Exception {
    String initiativeId = INITIATIVE_ID;
    FileReportDTO fileReportDTO = FileReportDTO.builder()
      .data("fake csv content".getBytes())
      .filename("test.csv")
      .build();

    Mockito.when(productFileService.downloadReport(TEST_ID_UPLOAD, "83843864-f3c0-4def-badb-7f197471b72e", initiativeId))
      .thenReturn(fileReportDTO);

    mockMvc.perform(get("/idpay/register/initiatives/{initiativeId}/product-files/{productFileId}/report", initiativeId, TEST_ID_UPLOAD)
        .header("x-organization-id", "83843864-f3c0-4def-badb-7f197471b72e"))
      .andExpect(status().isOk())
      .andExpect(header().string("Content-Disposition", "attachment; filename=test.csv"))
      .andExpect(content().bytes(fileReportDTO.getData()));
  }


  @Test
  void downloadCsv_notFound() throws Exception {
    String initiativeId = INITIATIVE_ID;
    String validOrgId = "83843864-f3c0-4def-badb-7f197471b72e";

    Mockito.when(productFileService.downloadReport(TEST_ID_UPLOAD, validOrgId, initiativeId))
      .thenThrow(new ReportNotFoundException("File not found"));

    mockMvc.perform(get("/idpay/register/initiatives/{initiativeId}/product-files/{productFileId}/report", initiativeId, TEST_ID_UPLOAD)
        .header("x-organization-id", validOrgId))
      .andExpect(status().isInternalServerError());
  }

  @Test
  void uploadProductFile_withInvalidExtension_KoStatus() throws Exception {
    MockMultipartFile wrongFile = new MockMultipartFile("csv", "file.txt", "text/plain", "data".getBytes());

    Mockito.when(productFileService.uploadFile(any(),any(), any(), any(), any(), any(), any()))
      .thenReturn(ProductFileResult.ko("EXTENSION_FILE_ERROR"));

    mockMvc.perform(multipart("/idpay/register/initiatives/{initiativeId}/product-files", INITIATIVE_ID)
        .file(wrongFile)
        .param("initiativeId", INITIATIVE_ID)
        .param("category", "eprel")
        .header("x-organization-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .header("x-user-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .header("x-user-email", "user@email.com")
        .header("x-organization-name", "org-name"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorKey").value("EXTENSION_FILE_ERROR"));
  }

  @Test
  void uploadProductFile_withInvalidHeader_KoStatus() throws Exception {
    MockMultipartFile file = new MockMultipartFile("csv", "p.csv", "text/csv", "wrong,header".getBytes());
    Mockito.when(productFileService.uploadFile(any(),any(), any(), any(), any(), any(), any()))
      .thenReturn(ProductFileResult.ko("HEADER_FILE_ERROR"));

    mockMvc.perform(multipart("/idpay/register/initiatives/{initiativeId}/product-files", INITIATIVE_ID)
        .file(file)
        .param("initiativeId", INITIATIVE_ID)
        .param("category", "eprel")
        .header("x-organization-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .header("x-user-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .header("x-user-email", "user@email.com")
        .header("x-organization-name", "org-name"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorKey").value("HEADER_FILE_ERROR"));
  }

  @Test
  void uploadProductFile_withTooManyRecords_KoStatus() throws Exception {
    MockMultipartFile file = new MockMultipartFile("csv", "big.csv", "text/csv", "id,name\n1,P".getBytes());
    Mockito.when(productFileService.uploadFile(any(),any(), any(), any(), any(), any(), any()))
      .thenReturn(ProductFileResult.ko("MAX_ROW_FILE_ERROR"));

    mockMvc.perform(multipart("/idpay/register/initiatives/{initiativeId}/product-files", INITIATIVE_ID)
        .file(file)
        .param("initiativeId", INITIATIVE_ID)
        .param("category", "eprel")
        .header("x-organization-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .header("x-user-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .header("x-user-email", "user@email.com")
        .header("x-organization-name", "org-name"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorKey").value("MAX_ROW_FILE_ERROR"));
  }

  @Test
  void uploadProductFile_withValidCsv_shouldReturnSuccess() throws Exception {
    MockMultipartFile file = new MockMultipartFile("csv", "valid.csv", "text/csv", "content".getBytes());

    Mockito.when(productFileService.uploadFile(any(),any(), any(), any(), any(), any(), any()))
      .thenReturn(ProductFileResult.ok());

    mockMvc.perform(multipart("/idpay/register/initiatives/{initiativeId}/product-files", INITIATIVE_ID)
        .file(file)
        .param("initiativeId", INITIATIVE_ID)
        .param("category", "eprel")
        .header("x-organization-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .header("x-user-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .header("x-user-email", "user@email.com")
        .header("x-organization-name", "org-name"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("OK"));
  }

  @Test
  void shouldReturn200AndListWhenOrganizationIdIsValid() throws Exception {
    String initiativeId = INITIATIVE_ID;
    Mockito.when(productFileService.retrieveDistinctProductFileIdsBasedOnRole(any(), any(), any(), any()))
      .thenReturn(List.of(new ProductBatchDTO("file123", "test.csv")));

    mockMvc.perform(get("/idpay/register/initiatives/{initiativeId}/product-files/batch-list", initiativeId)
        .header("x-organization-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .header("x-organization-role", "operatore")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].productFileId").value("file123"));
  }

  @Test
  void shouldReturn200WithEmptyListWhenNoFilesFound() throws Exception {
    String initiativeId = INITIATIVE_ID;
    Mockito.when(productFileService.retrieveDistinctProductFileIdsBasedOnRole(any(), any(), any(), any()))
      .thenReturn(List.of());

    mockMvc.perform(get("/idpay/register/initiatives/{initiativeId}/product-files/batch-list", initiativeId)
        .header("x-organization-role", "operatore")
        .header("x-organization-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json("[]"));
  }

  @Test
  void verifyProductFile_shouldReturnSuccess() throws Exception {
    MockMultipartFile file = new MockMultipartFile("csv", "verify.csv", "text/csv", "content".getBytes());

    Mockito.when(productFileService.validateFile(any(),any(), any(), any(), any(), any(), any()))
      .thenReturn(ProductFileResult.ok());

    mockMvc.perform(multipart("/idpay/register/initiatives/{initiativeId}/product-files/verify", INITIATIVE_ID)
        .file(file)
        .param("initiativeId", INITIATIVE_ID)
        .param("category", "eprel")
        .header("x-organization-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .header("x-user-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .header("x-user-email", "user@email.com")
        .header("x-organization-name", "org-name"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("OK"));
  }

  @Test
  void verifyProductFile_shouldReturnKoStatus_whenValidationFails() throws Exception {
    MockMultipartFile file = new MockMultipartFile("csv", "verify.csv", "text/csv", "bad,header".getBytes());
    Mockito.when(productFileService.validateFile(any(),any(), any(), any(), any(), any(), any()))
      .thenReturn(ProductFileResult.ko("INVALID_HEADER"));

    mockMvc.perform(multipart("/idpay/register/initiatives/{initiativeId}/product-files/verify", INITIATIVE_ID)
        .file(file)
        .param("initiativeId", INITIATIVE_ID)
        .param("category", "eprel")
        .header("x-organization-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .header("x-user-id", "83843864-f3c0-4def-badb-7f197471b72e")
        .header("x-user-email", "user@email.com")
        .header("x-organization-name", "org-name"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("KO"))
      .andExpect(jsonPath("$.errorKey").value("INVALID_HEADER"));
  }


}
