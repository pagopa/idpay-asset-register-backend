package it.gov.pagopa.register.connector.notification;

import it.gov.pagopa.register.dto.notification.EmailMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class NotificationServiceImpl implements  NotificationService {

  public static final String PRODUC_FILE_ID = "productFileName";
  private final NotificationRestClient notificationRestClient;
  @Value("${app.rest-client.email-notification.subject.ok}")
  private String subjectOk;

  @Value("${app.rest-client.email-notification.subject.partial}")
  private String subjectPartial;
  @Value("${app.rest-client.email-notification.template.ok}")
  private String templateOk;

  @Value("${app.rest-client.email-notification.template.partial}")
  private String templatePartial;
  public NotificationServiceImpl(NotificationRestClient notificationRestClient) {
    this.notificationRestClient = notificationRestClient;
  }

  @Override
  public void sendEmailOk(String productFileId, String senderEmail) {
    EmailMessageDTO email = getEmailMessageDTO(productFileId, templateOk, senderEmail, subjectOk);
    notificationRestClient.sendEmail(email);
  }

  @Override
  public void sendEmailPartial(String productFileId, String senderEmail) {
    EmailMessageDTO email = getEmailMessageDTO(productFileId, templatePartial, senderEmail, subjectPartial);
    notificationRestClient.sendEmail(email);
  }

  private EmailMessageDTO getEmailMessageDTO(String productFileId, String templatePartial, String recipientEmail, String subjectPartial) {
      return EmailMessageDTO.builder()
      .templateValues(Map.of(
        PRODUC_FILE_ID, productFileId))
      .templateName(templatePartial)
      .senderEmail(null)
      .subject(subjectPartial)
      .content(null)
      .recipientEmail(recipientEmail)
      .build();
  }
}
