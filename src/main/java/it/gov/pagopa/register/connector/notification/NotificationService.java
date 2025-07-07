package it.gov.pagopa.register.connector.notification;

import it.gov.pagopa.register.dto.notification.EmailMessageDTO;

public interface NotificationService  {
  void sendEmail(EmailMessageDTO body);
}
