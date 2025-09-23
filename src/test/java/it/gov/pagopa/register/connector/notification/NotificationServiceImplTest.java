package it.gov.pagopa.register.connector.notification;

import it.gov.pagopa.register.configuration.EmailNotificationConfig;
import it.gov.pagopa.register.dto.notification.EmailMessageDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@SpringBootTest(
  classes = {
    NotificationServiceImpl.class,
  }
)
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
    assertEquals("Prodotti elaborati con successo", email.getSubject());
    assertEquals("Email_RDB_EsitoProdottiOK", email.getTemplateName());
    assertEquals(Map.of("productFileName", PRODUCT_FILE_ID), email.getTemplateValues());
  }

  @Test
  void shouldSendEmailPartial() {
    notificationService.sendEmailPartial(PRODUCT_FILE_ID, SENDER_EMAIL);

    ArgumentCaptor<EmailMessageDTO> captor = ArgumentCaptor.forClass(EmailMessageDTO.class);
    verify(restClientMock, times(1)).sendEmail(captor.capture());

    EmailMessageDTO email = captor.getValue();
    assertEquals(SENDER_EMAIL, email.getRecipientEmail());
    assertEquals("Elaborazione parziale dei prodotti", email.getSubject());
    assertEquals("Email_RDB_EsitoProdottiParziale", email.getTemplateName());
    assertEquals(Map.of("productFileName", PRODUCT_FILE_ID), email.getTemplateValues());
  }


  @Test
  void shouldSendEmailUpdateStatus() {
    List<String> products = List.of("P001", "P002");
    String formalMotivation = "Motivazione di test";   // <-- rinominata
    String status = "REJECTED";
    String recipientEmail = "utente@example.it";
    notificationService.sendEmailUpdateStatus(products, formalMotivation, status, recipientEmail);

    ArgumentCaptor<EmailMessageDTO> captor = ArgumentCaptor.forClass(EmailMessageDTO.class);
    verify(restClientMock, times(1)).sendEmail(captor.capture());

    EmailMessageDTO email = captor.getValue();
    assertEquals(recipientEmail, email.getRecipientEmail());
    assertEquals("Prodotti Esclusi", email.getSubject());
    assertEquals("Email_RDB_EsclusioneProdotti", email.getTemplateName());

    String expectedHtmlList = "<li>P001</li><li>P002</li>";
    Map<String, String> expectedTemplateValues = Map.of(
      "excludedList", expectedHtmlList
    );

    assertEquals(expectedTemplateValues, email.getTemplateValues());



  }

}

