package it.gov.pagopa.register.controller.operation;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.register.dto.operation.ProductFileDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.service.operation.ProductFileService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductFileController.class)
class ProductFileControllerTestImpl {
  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ProductFileService productFileService;

  @Autowired
  private ObjectMapper objectMapper;

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

    Mockito.when(productFileService.downloadFilesByPage(eq("org123"), any(Pageable.class)))
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
    Mockito.when(productFileService.downloadFilesByPage(eq("org123"), any(Pageable.class)))
      .thenThrow(new RuntimeException("Service error"));

    mockMvc.perform(get("/idpay/register/product-files")
        .header("x-organization-id", "org123")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isInternalServerError());
  }
}
