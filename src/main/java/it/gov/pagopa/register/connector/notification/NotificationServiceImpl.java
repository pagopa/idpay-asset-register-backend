package it.gov.pagopa.register.connector.notification;

import it.gov.pagopa.register.configuration.EmailNotificationConfig;
import it.gov.pagopa.register.dto.notification.EmailMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.Map;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
  public static final String PARTIAL = "partial";
  public static final String OK = "ok";

  private final NotificationRestClient notificationRestClient;
  private final EmailNotificationConfig emailProps;

  public NotificationServiceImpl(NotificationRestClient notificationRestClient,
                                 EmailNotificationConfig emailProps) {
    this.notificationRestClient = notificationRestClient;
    this.emailProps = emailProps;
  }

  @Override
  public void sendEmailOk(String productFileId, String senderEmail) {
    EmailMessageDTO email = buildUploadEmailMessageDTO(
      productFileId,
      emailProps.getTemplate().get(OK),
      senderEmail,
      emailProps.getSubject().get(OK)
    );
    notificationRestClient.sendEmail(email);
  }

  @Override
  public void sendEmailPartial(String productFileId, String recipientEmail) {
    EmailMessageDTO email = buildUploadEmailMessageDTO(
      productFileId,
      emailProps.getTemplate().get(PARTIAL),
      recipientEmail,
      emailProps.getSubject().get(PARTIAL)
    );
    notificationRestClient.sendEmail(email);
  }

  @Override
  public void sendEmailUpdateStatus(String products, String motivation, String status, String recipientEmail) {
    EmailMessageDTO email = buildUpdateEmailMessageDTO(
      products,
      motivation,
      emailProps.getTemplate().get(status.toLowerCase()),
      recipientEmail,
      emailProps.getSubject().get(status.toLowerCase())
    );
    notificationRestClient.sendEmail(email);
  }

  //TODO Add tamplateValues
  private EmailMessageDTO buildUpdateEmailMessageDTO(String products, String motivation,String template, String recipientEmail, String subject) {
    return EmailMessageDTO.builder()
      .templateName(template)
      .senderEmail(null)
      .subject(subject)
      .content(null)
      .recipientEmail(recipientEmail)
      .build();
  }

  private EmailMessageDTO buildUploadEmailMessageDTO(String productFileId, String template, String recipientEmail, String subject) {
    return EmailMessageDTO.builder()
      .templateValues(Map.of(emailProps.getPlaceHolder().get(PARTIAL), productFileId))
      .templateName(template)
      .senderEmail(null)
      .subject(subject)
      .content(null)
      .recipientEmail(recipientEmail)
      .build();
  }
}
