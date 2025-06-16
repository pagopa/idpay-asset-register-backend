package it.gov.pagopa.register.config;


import it.gov.pagopa.register.connector.onetrust.OneTrustRestClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = {
        OneTrustRestClient.class
})
public class RestConnectorConfig {
}
