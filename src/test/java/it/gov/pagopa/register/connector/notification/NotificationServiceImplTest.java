package it.gov.pagopa.register.connector.notification;
import it.gov.pagopa.register.dto.notification.EmailMessageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

  private NotificationRestClient restClientMock;
  private NotificationServiceImpl notificationService;

  private static final String PRODUCT_FILE_ID = "file_123.csv";
  private static final String SENDER_EMAIL = "ente@example.it";
  private static final String SUBJECT_OK = "Subject OK";
  private static final String SUBJECT_PARTIAL = "Subject Partial";
  private static final String TEMPLATE_OK = "template-ok";
  private static final String TEMPLATE_PARTIAL = "template-partial";

  @BeforeEach
  void setup() {
    restClientMock = mock(NotificationRestClient.class);
    notificationService = new NotificationServiceImpl(restClientMock);
    ReflectionTestUtils.setField(notificationService, "subjectOk", SUBJECT_OK);
    ReflectionTestUtils.setField(notificationService, "subjectPartial", SUBJECT_PARTIAL);
    ReflectionTestUtils.setField(notificationService, "templateOk", TEMPLATE_OK);
    ReflectionTestUtils.setField(notificationService, "templatePartial", TEMPLATE_PARTIAL);
  }

  @Test
  void shouldSendEmailOk() {
    notificationService.sendEmailOk(PRODUCT_FILE_ID, SENDER_EMAIL);

    ArgumentCaptor<EmailMessageDTO> captor = ArgumentCaptor.forClass(EmailMessageDTO.class);
    verify(restClientMock, times(1)).sendEmail(captor.capture());

    EmailMessageDTO email = captor.getValue();
    assertEquals(SENDER_EMAIL, email.getRecipientEmail());
    assertEquals(SUBJECT_OK, email.getSubject());
    assertEquals(TEMPLATE_OK, email.getTemplateName());
    assertEquals(Map.of("productFileName", PRODUCT_FILE_ID), email.getTemplateValues());
  }

  @Test
  void shouldSendEmailPartial() {
    notificationService.sendEmailPartial(PRODUCT_FILE_ID, SENDER_EMAIL);

    ArgumentCaptor<EmailMessageDTO> captor = ArgumentCaptor.forClass(EmailMessageDTO.class);
    verify(restClientMock, times(1)).sendEmail(captor.capture());

    EmailMessageDTO email = captor.getValue();
    assertEquals(SENDER_EMAIL, email.getRecipientEmail());
    assertEquals(SUBJECT_PARTIAL, email.getSubject());
    assertEquals(TEMPLATE_PARTIAL, email.getTemplateName());
    assertEquals(Map.of("productFileName", PRODUCT_FILE_ID), email.getTemplateValues());
  }
}
