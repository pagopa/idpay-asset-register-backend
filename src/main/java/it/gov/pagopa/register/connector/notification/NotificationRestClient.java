package it.gov.pagopa.register.connector.notification;

import it.gov.pagopa.register.dto.notification.EmailMessageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "${app.rest-client.email-notification.service.name}", url = "${app.rest-client.email-notification.service.base-url}")
public interface NotificationRestClient {

  @PostMapping(value = "/idpay/email-notification/notify", consumes = "application/json")
  ResponseEntity<Void> sendEmail(
    @RequestBody EmailMessageDTO body);
}
