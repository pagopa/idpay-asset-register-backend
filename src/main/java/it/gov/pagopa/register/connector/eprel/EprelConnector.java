package it.gov.pagopa.register.connector.eprel;

import it.gov.pagopa.register.utils.EprelProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Component
public class EprelConnector {

    private final String eprelUrl;
    private final RestTemplate restTemplate;

    public EprelConnector(@Value("${app.rest-client.eprel.service.get-product}") String eprelUrl,
                          RestTemplate restTemplate) {
      this.eprelUrl = eprelUrl;
      this.restTemplate = restTemplate;
    }

    public EprelProduct callEprel(String registrationNumber) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(eprelUrl)
                    .buildAndExpand(registrationNumber)
                    .toUri();
            ResponseEntity<EprelProduct> response = restTemplate.getForEntity(uri, EprelProduct.class);
            return response.getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }
}



