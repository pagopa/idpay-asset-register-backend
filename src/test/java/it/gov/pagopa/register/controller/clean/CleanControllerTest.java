package it.gov.pagopa.register.controller.clean;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.register.dto.clean.CleanRequestDTO;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.service.clean.CleanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.USERNAME;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(CleanController.class)
class CleanControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CleanService cleanService;

  @Autowired
  private ObjectMapper objectMapper;

  private static final CleanRequestDTO cleanRequestDTO = new CleanRequestDTO(List.of("TEST"));


  @Test
  void testRemoveProducts() throws Exception {
    mockMvc.perform(
      org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .post("/idpay/register/clean/products")
        .header("x-organization-role", UserRole.INVITALIA_ADMIN.getRole())
        .header("x-user-name", USERNAME)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(cleanRequestDTO)))
      .andExpect(status().isNoContent());
  }

  @Test
  void testRemoveProductsFile() throws Exception {
    mockMvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
          .post("/idpay/register/clean/products-file")
          .header("x-organization-role", UserRole.INVITALIA_ADMIN.getRole())
          .header("x-user-name", USERNAME)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(cleanRequestDTO)))
      .andExpect(status().isNoContent());
  }

  @Test
  void testRemoveReportFileFromStorage() throws Exception {
    mockMvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
          .post("/idpay/register/clean/products-file/report")
          .header("x-organization-role", UserRole.INVITALIA_ADMIN.getRole())
          .header("x-user-name", USERNAME)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(cleanRequestDTO)))
      .andExpect(status().isNoContent());
  }

  @Test
  void testRemoveFormalFileFromStorage() throws Exception {
    mockMvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
          .post("/idpay/register/clean/products-file/formal")
          .header("x-organization-role", UserRole.INVITALIA_ADMIN.getRole())
          .header("x-user-name", USERNAME)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(cleanRequestDTO)))
      .andExpect(status().isNoContent());
  }





}





