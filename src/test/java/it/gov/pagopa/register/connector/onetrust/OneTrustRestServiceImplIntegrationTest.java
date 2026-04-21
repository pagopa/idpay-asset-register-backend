package it.gov.pagopa.register.connector.onetrust;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ContextConfiguration(classes = {
  OneTrustRestClient.class,
  OneTrustRestService.class,
  OneTrustRestServiceImpl.class,
  FeignAutoConfiguration.class,
  HttpMessageConvertersAutoConfiguration.class
})
@EnableFeignClients(clients = OneTrustRestClient.class)
class OneTrustRestServiceImplIntegrationTest {

  private static WireMockServer wireMockServer;

  private static final String EXPECTED_VERSION_ID = "mock-version-id";

  @BeforeAll
  static void setUp() {
    wireMockServer = new WireMockServer(options().dynamicPort());
    wireMockServer.start();
  }

  @AfterAll
  static void tearDown() {
    wireMockServer.stop();
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add(
      "app.rest-client.one-trust.service.privacy-notices.base-url",
      wireMockServer::baseUrl
    );
  }

  @Autowired
  private OneTrustRestService service;

  @Test
  void test() {

    String tosId = "TOSID_OK";

    wireMockServer.stubFor(get(urlPathEqualTo("/privacynotices/TOSID_OK"))
      .withQueryParam("date", matching(".*"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("""
                  {
                  "version": {
                    "id": "mock-version-id",
                    "name": "test",
                    "publishedDate": "2026-04-14T10:00:00",
                    "status": "ACTIVE",
                    "version": 1
                  }
                }
                """)));

    var result = service.getPrivacyNotices(tosId);

    assertNotNull(result);
    assertEquals(EXPECTED_VERSION_ID, result.getVersion().getId());
  }
}
