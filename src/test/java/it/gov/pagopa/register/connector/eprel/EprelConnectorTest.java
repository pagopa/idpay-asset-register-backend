package it.gov.pagopa.register.connector.eprel;

import it.gov.pagopa.register.configuration.RestTemplateConfig;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
  EprelConnector.class,
  RestTemplateConfig.class
})
@TestPropertySource(properties = {
  "app.rest-client.eprel.service.get-product=http://localhost/{REGISTRATION_NUMBER}",
})
class EprelConnectorTest {

  public static final String REGISTRATION_NUMBER = "12345";
  @MockitoBean
  private RestTemplate restTemplate;
  @Autowired
  private EprelConnector eprelConnector;


  @Test
  void callEprel_OK() throws Exception {
    EprelProduct mockProduct = new EprelProduct();
    mockProduct.setEprelRegistrationNumber(REGISTRATION_NUMBER);

    when(restTemplate.getForEntity(any(), any()))
      .thenReturn(new ResponseEntity<>(mockProduct, HttpStatus.OK));

    EprelProduct result = eprelConnector.callEprel(REGISTRATION_NUMBER);

    assertThat(result).isNotNull().isEqualTo(mockProduct);
  }

  @Test
  void callEprel_shouldThrowNotFoundException() {
    when(restTemplate.getForEntity(any(), any()))
      .thenThrow(new HttpClientErrorException(NOT_FOUND));

    assertThatThrownBy(() -> eprelConnector.callEprel(REGISTRATION_NUMBER))
      .isInstanceOf(HttpClientErrorException.class)
      .hasMessageContaining("404")
      .satisfies(ex -> {
        HttpClientErrorException httpEx = (HttpClientErrorException) ex;
        assertThat(httpEx.getStatusCode()).isEqualTo(NOT_FOUND);
      });
  }

  @Test
  void callEprel_shouldThrowGenericException() {
    when(restTemplate.getForEntity(any(), any()))
      .thenThrow(new RuntimeException("Generic error"));

    assertThatThrownBy(() -> eprelConnector.callEprel(REGISTRATION_NUMBER))
      .isInstanceOf(RuntimeException.class)
      .hasMessage("Generic error");
  }

}
