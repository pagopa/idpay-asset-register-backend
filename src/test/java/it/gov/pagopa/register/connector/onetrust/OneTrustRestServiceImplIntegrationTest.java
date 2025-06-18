package it.gov.pagopa.register.connector.onetrust;

import com.github.tomakehurst.wiremock.WireMockServer;
import it.gov.pagopa.register.dto.onetrust.PrivacyNoticesDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(classes = {
        OneTrustRestClient.class,
        OneTrustRestService.class,
        OneTrustRestServiceImpl.class,
        FeignAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class
})
@TestPropertySource(properties = {
        "app.rest-client.one-trust.service.privacy-notices.base-url=http://localhost:${wiremock.server.port}"
})
@AutoConfigureWireMock(stubs = "classpath:/stub", port = 0)
@EnableFeignClients(clients = {
  OneTrustRestClient.class
})
class OneTrustRestServiceImplIntegrationTest {
    private static final String EXPECTED_VERSION_ID = "mock-version-id";

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private OneTrustRestService service;

    @Test
    void test() {
        String tosId = "TOSID_OK";

        PrivacyNoticesDTO result = service.getPrivacyNotices(tosId);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(EXPECTED_VERSION_ID, result.getVersion().getId());
    }
}
