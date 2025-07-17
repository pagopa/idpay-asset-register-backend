package it.gov.pagopa.register.connector.onetrust;

import it.gov.pagopa.register.dto.onetrust.PrivacyNoticesDTO;
import it.gov.pagopa.register.dto.onetrust.PrivacyNoticesVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestPropertySource(
        locations = "classpath:application.yml",
        properties = {
                "app.rest-client.one-trust.service.privacy-notices.apiKey=fake-api-key"
        })
@SpringJUnitConfig(classes = {OneTrustRestServiceImpl.class})
class OneTrustRestServiceImplTest {

    private static final String TOS_ID = "TOS_ID";
    public static final String VERSION_ID = "VERSION_ID";
    public static final String API_KEY_BEARER_TOKEN = "Bearer API_KEY_BEARER_TOKEN";

    @MockitoBean
    private OneTrustRestClient oneTrustRestClient;
    @Autowired
    private OneTrustRestService oneTrustRestService;

    @Test
    void test() {
        PrivacyNoticesDTO response = PrivacyNoticesDTO.builder()
                .version(
                        PrivacyNoticesVersion.builder()
                                .id(VERSION_ID).build()
                )
                .build();
        Mockito.when(oneTrustRestClient.getPrivacyNotices(Mockito.eq(TOS_ID), Mockito.anyString(), Mockito.startsWith("Bearer "))).thenReturn(response);

        PrivacyNoticesDTO result = oneTrustRestService.getPrivacyNotices(TOS_ID);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(response.getVersion().getId(), result.getVersion().getId());

        verify(oneTrustRestClient, times(1)).getPrivacyNotices(Mockito.eq(TOS_ID), Mockito.anyString(), Mockito.startsWith("Bearer "));
    }
}
