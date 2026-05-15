package it.gov.pagopa.register.configuration;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.register.constants.ExceptionConstants;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.exception.role.ConsentNotFoundException;
import it.gov.pagopa.register.exception.role.PermissionNotFoundException;
import it.gov.pagopa.register.exception.role.VersionNotMatchedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RegisterConfigurationTest {

  @Test
  void errorManagerConfig_exposesExpectedErrorDtos() {
    ErrorManagerConfig config = new ErrorManagerConfig();

    ErrorDTO defaultError = config.defaultErrorDTO();
    ErrorDTO tooManyRequests = config.tooManyRequestsErrorDTO();
    ErrorDTO invalidRequest = config.templateValidationErrorDTO();

    assertEquals(ExceptionConstants.ExceptionCode.GENERIC_ERROR, defaultError.getCode());
    assertEquals("A generic error occurred", defaultError.getMessage());
    assertEquals(ExceptionConstants.ExceptionCode.TOO_MANY_REQUESTS, tooManyRequests.getCode());
    assertEquals("Too Many Requests", tooManyRequests.getMessage());
    assertEquals(ExceptionConstants.ExceptionCode.INVALID_REQUEST, invalidRequest.getCode());
    assertNull(invalidRequest.getMessage());
  }

  @Test
  void serviceExceptionConfig_mapsDomainExceptionsToHttpStatuses() {
    Map<Class<? extends ServiceException>, HttpStatus> mapper =
      new ServiceExceptionConfig().serviceExceptionMapper();

    assertEquals(HttpStatus.NOT_FOUND, mapper.get(PermissionNotFoundException.class));
    assertEquals(HttpStatus.NOT_FOUND, mapper.get(ReportNotFoundException.class));
    assertEquals(HttpStatus.NOT_FOUND, mapper.get(ConsentNotFoundException.class));
    assertEquals(HttpStatus.BAD_REQUEST, mapper.get(VersionNotMatchedException.class));
  }

  @Test
  void simpleBeanConfigs_createReusableBeans() {
    RestTemplate restTemplate = new RestTemplateConfig().restTemplate();
    ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    assertNotNull(restTemplate);
    assertNotNull(objectMapper);
  }

  @Test
  void propertiesConfigs_storeValues() {
    ProductFileValidationConfig validationConfig = new ProductFileValidationConfig();
    validationConfig.setMaxRows(10);
    validationConfig.setMaxSize(20);

    EmailNotificationConfig emailConfig = new EmailNotificationConfig();
    EmailNotificationConfig.Service service = new EmailNotificationConfig.Service();
    service.setName("email");
    service.setBaseUrl("http://localhost");
    emailConfig.setService(service);
    emailConfig.setTemplate(Map.of("ok", "template"));
    emailConfig.setSubject(Map.of("ok", "subject"));
    emailConfig.setPlaceHolder(Map.of("name", "value"));

    assertEquals(10, validationConfig.getMaxRows());
    assertEquals(20, validationConfig.getMaxSize());
    assertEquals("email", emailConfig.getService().getName());
    assertEquals("http://localhost", emailConfig.getService().getBaseUrl());
    assertEquals("template", emailConfig.getTemplate().get("ok"));
    assertEquals("subject", emailConfig.getSubject().get("ok"));
    assertEquals("value", emailConfig.getPlaceHolder().get("name"));
  }
}
