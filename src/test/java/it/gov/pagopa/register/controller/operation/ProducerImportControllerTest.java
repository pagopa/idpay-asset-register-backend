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

import static org.mockito.ArgumentMatchers.anyList;
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
    Mockito.when(producerImportService.importProducers(anyList()))
      .thenReturn(ProducerImportResultDTO.builder()
        .status("OK")
        .totalRecords(2)
        .importedRecords(2)
        .failedRecords(0)
        .message("Producer import completed successfully")
        .build());

    mockMvc.perform(post("/idpay/register/producers")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "producers": [
              {"producerId":"456","initiativeId":"111","producerName":"Producer 1","producerEmail":"producer1@test.it"},
              {"producerId":"678","initiativeId":"222","producerName":"Producer 2","producerEmail":"producer2@test.it"}
            ]
          }
          """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("OK"))
      .andExpect(jsonPath("$.totalRecords").value(2))
      .andExpect(jsonPath("$.importedRecords").value(2))
      .andExpect(jsonPath("$.failedRecords").value(0))
      .andExpect(jsonPath("$.message").value("Producer import completed successfully"));
  }
}
