package it.gov.pagopa.register.connector.notification;

import it.gov.pagopa.register.dto.notification.EmailMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationServiceImpl implements  NotificationService {

  private final NotificationRestClient notificationRestClient;

  public NotificationServiceImpl(NotificationRestClient notificationRestClient) {
    this.notificationRestClient = notificationRestClient;
  }

  @Override
  public void sendEmail(EmailMessageDTO body) {
    notificationRestClient.sendEmail(body);
  }
}
