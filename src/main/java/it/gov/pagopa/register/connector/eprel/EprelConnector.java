package it.gov.pagopa.register.connector.eprel;

import it.gov.pagopa.register.utils.EprelProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Component
public class EprelConnector {

    private static final String EPREL_URL = "https://eprel.ec.europa.eu/api/product/{REGISTRATION_NUMBER}";
    private final RestTemplate restTemplate;

    public EprelConnector() {
        this.restTemplate = new RestTemplate();
    }

    public EprelProduct callEprel(String registrationNumber) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(EPREL_URL)
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



