package it.gov.pagopa.register.connector.notification;

import java.util.List;

public interface NotificationService  {
  void sendEmailOk(String initiativeName, String productFileId, String recipientEmail);

  void sendEmailPartial(String initiativeName, String productFileId, String recipientEmail);

  void sendEmailUpdateStatus(String initiativeName, List<String> products, String formalMotivation, String status, String recipientEmail);
}
