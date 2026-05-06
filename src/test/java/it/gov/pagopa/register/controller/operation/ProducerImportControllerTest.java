package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.service.operation.ProducerImportService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProducerImportController.class)
class ProducerImportControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProducerImportService producerImportService;

  @Test
  void importProducers_shouldReturnImportedRecords() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
      "csv",
      "produttori.csv",
      "text/csv",
      "producerId,initiativeId\n456,111".getBytes()
    );

    Mockito.when(producerImportService.importCsv(any()))
      .thenReturn(ProducerImportResultDTO.builder()
        .status("OK")
        .importedRecords(1)
        .build());

    mockMvc.perform(multipart("/idpay/register/producers/import-csv")
        .file(file)
        .contentType(MediaType.MULTIPART_FORM_DATA))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("OK"))
      .andExpect(jsonPath("$.importedRecords").value(1));
  }

  @Test
  void importProducersJson_shouldReturnImportedRecords() throws Exception {
    Mockito.when(producerImportService.importJson(anyString()))
      .thenReturn(ProducerImportResultDTO.builder()
        .status("OK")
        .importedRecords(2)
        .build());

    mockMvc.perform(post("/idpay/register/producers/import-json")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"_id":"456_111","producerId":"456","initiativeId":"111"}
          {"_id":"678_111","producerId":"678","initiativeId":"111"}
          """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("OK"))
      .andExpect(jsonPath("$.importedRecords").value(2));
  }
}
