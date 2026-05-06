package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.service.operation.ProducerImportService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {ProducerImportController.class}, excludeAutoConfiguration = {
  UserDetailsServiceAutoConfiguration.class,
  SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class ProducerImportControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProducerImportService producerImportService;

  @Test
  void importProducers_shouldReturnImportedRecords() throws Exception {
    Mockito.when(producerImportService.importJson(anyString()))
      .thenReturn(ProducerImportResultDTO.builder()
        .status("OK")
        .importedRecords(2)
        .build());

    mockMvc.perform(post("/idpay/register/producers")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT"}
          {"producerId":"678","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567891","initiativeOrganizationName":"MEF"}
          """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("OK"))
      .andExpect(jsonPath("$.importedRecords").value(2));
  }
}
