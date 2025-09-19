package it.gov.pagopa.register.connector.notification;

import it.gov.pagopa.register.configuration.EmailNotificationConfig;
import it.gov.pagopa.register.dto.notification.EmailMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;

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
  public void sendEmailUpdateStatus(List<String> products, String formalMotivation, String status, String recipientEmail) {
    EmailMessageDTO email = buildUpdateEmailMessageDTO(
      status,
      products,
      formalMotivation,
      emailProps.getTemplate().get(status.toLowerCase()),
      recipientEmail,
      emailProps.getSubject().get(status.toLowerCase())
    );
    notificationRestClient.sendEmail(email);
  }

  private EmailMessageDTO buildUpdateEmailMessageDTO(String status, List<String> products, String formalMotivation, String template, String recipientEmail, String subject) {
    Map<String, String> templateValues = new HashMap<>();

    List<String> placeholders = Optional.ofNullable(emailProps.getPlaceHolder().get(status.toLowerCase()))
      .map(ph -> Arrays.asList(ph.split(",")))
      .orElse(List.of());

    for (String rawPlaceholder : placeholders) {
      String placeholder = rawPlaceholder.trim();

      switch (placeholder) {
        case "excludedList", "suspendedList", "ripristinedList" -> {
          String htmlList = products.stream()
            .map(code -> "<li>" + code + "</li>")
            .collect(Collectors.joining(""));
          templateValues.put(placeholder, htmlList);
        }
        case "formalMotivation" -> templateValues.put("formalMotivation", formalMotivation);
        default ->
          log.warn("Placeholder not exists: {}", placeholder);

      }
    }

    return EmailMessageDTO.builder()
      .templateName(template)
      .templateValues(templateValues)
      .subject(subject)
      .content(null)
      .senderEmail(null)
      .recipientEmail(recipientEmail)
      .build();
  }


  private EmailMessageDTO buildUploadEmailMessageDTO(String productFileId, String template, String recipientEmail, String subject) {
    return EmailMessageDTO.builder()
      .templateName(template)
      .templateValues(Map.of(emailProps.getPlaceHolder().get(PARTIAL), productFileId))
      .subject(subject)
      .senderEmail(null)
      .content(null)
      .recipientEmail(recipientEmail)
      .build();
  }
}
