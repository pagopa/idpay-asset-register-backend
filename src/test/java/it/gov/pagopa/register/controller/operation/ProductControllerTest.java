package it.gov.pagopa.register.controller.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.service.operation.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

      Mockito.when(productService.getProducts(eq("organizationIdTest")
          , any()
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
      Mockito.when(productService.getProducts(eq("organizationIdTest")
          , any()
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



  }


