package it.gov.pagopa.register.controller.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.dto.operation.ProductUpdateStatusRequestDTO;
import it.gov.pagopa.register.dto.operation.UpdateResultDTO;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
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

import static it.gov.pagopa.register.constants.AssetRegisterConstants.USERNAME;
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
      productDTO.setOrganizationId("83843864-f3c0-4def-badb-7f197471b72e");

      ProductListDTO mockResponse = ProductListDTO.builder()
        .content(Collections.singletonList(productDTO))
        .pageNo(0)
        .pageSize(10)
        .totalElements(1L)
        .totalPages(1)
        .build();

      when(productService.fetchProductsByFilters(eq("83843864-f3c0-4def-badb-7f197471b72e")
          , any()
          , any()
          , any()
          , any()
          , any()
          , any()
          , any()
          , any()
        ))
        .thenReturn(mockResponse);
      mockMvc.perform(get("/idpay/register/products")
          .queryParam("organizationId", "83843864-f3c0-4def-badb-7f197471b72e")
          .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.pageNo").value(0))
        .andExpect(jsonPath("$.pageSize").value(10))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));

    }


    //Test in caso di eccezione
    @Test
    void testGetProducts_ServiceThrowsException() throws Exception {
      when(productService.fetchProductsByFilters(eq("83843864-f3c0-4def-badb-7f197471b72e")
          , any()
          , any()
          , any()
          , any()
          , any()
          , any()
          , any()
          , any()
        ))
        .thenThrow(new RuntimeException("Service error"));

      mockMvc.perform(get("/idpay/register/products")
          .queryParam("organizationId", "83843864-f3c0-4def-badb-7f197471b72e")
          .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());
    }

    @Test
    void testUpdateProductStatuses_Success() throws Exception {

      UpdateResultDTO mockResponse = UpdateResultDTO.ok();

      List<String> productIds = List.of("prod-1", "prod-2");
      ProductUpdateStatusRequestDTO requestDTO = new ProductUpdateStatusRequestDTO();
      requestDTO.setGtinCodes(productIds);
      requestDTO.setCurrentStatus(ProductStatus.WAIT_APPROVED);
      requestDTO.setTargetStatus(ProductStatus.APPROVED);
      requestDTO.setMotivation("Valid reason");

      String requestBody = objectMapper.writeValueAsString(requestDTO);

      when(productService.updateProductStatusesWithNotification(
        productIds,
        ProductStatus.WAIT_APPROVED,
        ProductStatus.APPROVED,
        "Valid reason",
        UserRole.INVITALIA_ADMIN.getRole(),
        USERNAME
      )).thenReturn(mockResponse);

      mockMvc.perform(
          org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post("/idpay/register/products/update-status")
            .header("x-organization-role", UserRole.INVITALIA_ADMIN.getRole())
            .header("x-organization-username", USERNAME)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void testUpdateProductStatuses_MissingHeader() throws Exception {
      ProductUpdateStatusRequestDTO requestDTO = new ProductUpdateStatusRequestDTO();
      requestDTO.setGtinCodes(List.of("prod-1"));
      requestDTO.setCurrentStatus(ProductStatus.SUPERVISED);
      requestDTO.setTargetStatus(ProductStatus.SUPERVISED);
      requestDTO.setMotivation("Missing header test");

      String requestBody = objectMapper.writeValueAsString(requestDTO);

      mockMvc.perform(MockMvcRequestBuilders
          .post("/idpay/register/products/update-status")
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody))
        .andExpect(status().isBadRequest());
    }
}





