package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.service.operation.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(ProductController.class)
class ProductControllerImplTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ProductService productService;



  private static final  String TEST_ID_UPLOAD = "example_upload";

  @Test
  void downloadCsv_successfulResponse() throws Exception {
    ByteArrayOutputStream file = new ByteArrayOutputStream();
    file.write("fake csv content".getBytes());

    Mockito.when(productService.downloadReport(TEST_ID_UPLOAD)).thenReturn(file);

    mockMvc.perform(get("/idpay/register/download/report/{idUpload}", TEST_ID_UPLOAD)
        .param("idProduttore", "testProducer")
        .param("orgName", "testOrg"))
      .andExpect(status().isOk())
      .andExpect(header().string("Content-Disposition", "attachment; filename=expenseFiles.zip"))
      .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
      .andExpect(content().bytes(file.toByteArray()));
  }

  @Test
  void downloadCsv_notFound() throws Exception {
    Mockito.when(productService.downloadReport(TEST_ID_UPLOAD))
      .thenThrow(new RuntimeException("File not found"));

    mockMvc.perform(get("/idpay/register/download/report/{idUpload}", TEST_ID_UPLOAD))
      .andExpect(status().isInternalServerError());
  }
}
