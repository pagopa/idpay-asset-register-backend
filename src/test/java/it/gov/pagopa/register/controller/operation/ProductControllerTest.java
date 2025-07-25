package it.gov.pagopa.register.controller.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.dto.operation.ProductUpdateStatusRequestDTO;
import it.gov.pagopa.register.enums.ProductStatusEnum;
import it.gov.pagopa.register.service.operation.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;




  @WebMvcTest(ProductController.class)
  class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    //Test con esito positivo
    @Test
    void testGetProducts_Success() throws Exception {
      ProductDTO productDTO = new ProductDTO();
      productDTO.setOrganizationId("organizationIdTest");

      ProductListDTO mockResponse = ProductListDTO.builder()
        .content(Collections.singletonList(productDTO))
        .pageNo(0)
        .pageSize(10)
        .totalElements(1L)
        .totalPages(1)
        .build();

      when(productService.getProducts(eq("organizationIdTest")
          , any()
          , any()
          , any()
          , any()
          , any()
          , any()))
        .thenReturn(mockResponse);
      mockMvc.perform(get("/idpay/register/products")
          .header("x-organization-id", "organizationIdTest")
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
    void testGetProducts_MissingHeader() throws Exception {
      mockMvc.perform(get("/idpay/register/products")
          .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
    }

    //Test in caso di eccezione
    @Test
    void testGetProducts_ServiceThrowsException() throws Exception {
      when(productService.getProducts(eq("organizationIdTest")
          , any()
          , any()
          , any()
          , any()
          , any()
          , any()))
        .thenThrow(new RuntimeException("Service error"));

      mockMvc.perform(get("/idpay/register/products")
          .header("x-organization-id", "organizationIdTest")
          .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());
    }

    @Test
    void testUpdateProductStatuses_Success() throws Exception {

      ProductListDTO mockResponse = ProductListDTO.builder()
        .content(Collections.emptyList())
        .pageNo(0)
        .pageSize(0)
        .totalElements(0L)
        .totalPages(1)
        .build();


      List<String> productIds = List.of("prod-1", "prod-2");
      ProductUpdateStatusRequestDTO requestDTO = new ProductUpdateStatusRequestDTO();
      requestDTO.setGtinCodes(productIds);
      requestDTO.setStatus(ProductStatusEnum.APPROVED);
      requestDTO.setMotivation("Valid reason");

      String requestBody = objectMapper.writeValueAsString(requestDTO);

      when(productService.updateProductState(
        "org-test",
        productIds,
        ProductStatusEnum.APPROVED,
        "Valid reason"
      )).thenReturn(mockResponse);

      mockMvc.perform(
          org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .patch("/idpay/register/products/update-status")
            .header("x-organization-id", "org-test")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pageNo").value(0));
    }

    @Test
    void testUpdateProductStatuses_MissingHeader() throws Exception {
      ProductUpdateStatusRequestDTO requestDTO = new ProductUpdateStatusRequestDTO();
      requestDTO.setGtinCodes(List.of("prod-1"));
      requestDTO.setStatus(ProductStatusEnum.SUPERVISIONED);
      requestDTO.setMotivation("Missing header test");

      String requestBody = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(MockMvcRequestBuilders
          .patch("/idpay/register/products/update-status")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
        .andExpect(status().isBadRequest());
    }
}





