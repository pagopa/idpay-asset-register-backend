package it.gov.pagopa.register.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.rest-client.email-notification")
public class EmailNotificationConfig {
  private Service service;
  private Map<String, String> template;
  private Map<String, String> subject;
  private Map<String, String> placeHolder;

  @Getter
  @Setter
  public static class Service {
    private String name;
    private String baseUrl;
  }
}
