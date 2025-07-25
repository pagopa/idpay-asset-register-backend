package it.gov.pagopa.register.connector.notification;

public interface NotificationService  {
  void sendEmailOk(String productFileId, String recipientEmail);

  void sendEmailPartial(String productFileId, String recipientEmail);

  void sendEmailUpdateStatus(String products, String motivation, String status, String recipientEmail);
}
