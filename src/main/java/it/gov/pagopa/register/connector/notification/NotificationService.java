package it.gov.pagopa.register.connector.notification;

public interface NotificationService  {
  void sendEmailOk(String productFileId, String senderEmail);

  void sendEmailPartial(String productFileId, String senderEmail);
}
