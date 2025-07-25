package it.gov.pagopa.register.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.rest-client.email-notification")
public class EmailNotificationConfig {

  private Service service;
  private Map<String, String> template;
  private Map<String, String> subject;
  private Map<String, String> placeHolder;

  public static class Service {
    private String name;
    private String baseUrl;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
  }

  public Service getService() { return service; }
  public void setService(Service service) { this.service = service; }

  public Map<String, String> getTemplate() { return template; }
  public void setTemplate(Map<String, String> template) { this.template = template; }

  public Map<String, String> getSubject() { return subject; }
  public void setSubject(Map<String, String> subject) { this.subject = subject; }

  public Map<String, String> getPlaceHolder() { return placeHolder; }
  public void setPlaceHolder(Map<String, String> placeHolder) { this.placeHolder = placeHolder; }
}
