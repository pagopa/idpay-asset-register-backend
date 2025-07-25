package it.gov.pagopa.register.connector.notification;

import it.gov.pagopa.register.configuration.EmailNotificationConfig;
import it.gov.pagopa.register.dto.notification.EmailMessageDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@SpringBootTest(
  classes = {
    NotificationServiceImpl.class,
  },
  properties = {
  "app.rest-client.email-notification.template.ok=template-ok",
  "app.rest-client.email-notification.template.partial=template-partial",
  "app.rest-client.email-notification.subject.ok=Subject OK",
  "app.rest-client.email-notification.subject.partial=Subject Partial",
  "app.rest-client.email-notification.place-holder.ok=productFileName",
  "app.rest-client.email-notification.place-holder.partial=productFileName",
  "app.rest-client.email-notification.service.name=notificationClient",
  "app.rest-client.email-notification.service.base-url=http://localhost"
})
@EnableConfigurationProperties(EmailNotificationConfig.class)
class NotificationServiceImplTest {

  @MockitoBean
  private NotificationRestClient restClientMock;

  @Autowired
  private NotificationServiceImpl notificationService;

  @Autowired
  private EmailNotificationConfig emailNotificationConfig;

  private static final String PRODUCT_FILE_ID = "file_123.csv";
  private static final String SENDER_EMAIL = "ente@example.it";

  @Test
  void shouldSendEmailOk() {
    notificationService.sendEmailOk(PRODUCT_FILE_ID, SENDER_EMAIL);

    ArgumentCaptor<EmailMessageDTO> captor = ArgumentCaptor.forClass(EmailMessageDTO.class);
    verify(restClientMock, times(1)).sendEmail(captor.capture());

    EmailMessageDTO email = captor.getValue();
    assertEquals(SENDER_EMAIL, email.getRecipientEmail());
    assertEquals("Subject OK", email.getSubject());
    assertEquals("template-ok", email.getTemplateName());
    assertEquals(Map.of("productFileName", PRODUCT_FILE_ID), email.getTemplateValues());
  }

  @Test
  void shouldSendEmailPartial() {
    notificationService.sendEmailPartial(PRODUCT_FILE_ID, SENDER_EMAIL);

    ArgumentCaptor<EmailMessageDTO> captor = ArgumentCaptor.forClass(EmailMessageDTO.class);
    verify(restClientMock, times(1)).sendEmail(captor.capture());

    EmailMessageDTO email = captor.getValue();
    assertEquals(SENDER_EMAIL, email.getRecipientEmail());
    assertEquals("Subject Partial", email.getSubject());
    assertEquals("template-partial", email.getTemplateName());
    assertEquals(Map.of("productFileName", PRODUCT_FILE_ID), email.getTemplateValues());
  }
}

