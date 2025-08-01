package it.gov.pagopa.register.connector.notification;

import java.util.List;

public interface NotificationService  {
  void sendEmailOk(String productFileId, String recipientEmail);

  void sendEmailPartial(String productFileId, String recipientEmail);

  void sendEmailUpdateStatus(List<String> products, String motivation, String status, String recipientEmail);
}
