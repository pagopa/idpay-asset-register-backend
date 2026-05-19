package it.gov.pagopa.register.configuration;


import it.gov.pagopa.register.connector.notification.NotificationServiceImpl;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import it.gov.pagopa.register.repository.initiative.InitiativeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(
  classes = {
    InitiativeConfigMap.class,
  }
)
class InitiativeConfigMapSpringTest {

  @MockitoBean
  private InitiativeRepository repository;

  @Autowired
  private InitiativeConfigMap configMap;

  @Test
  void shouldLoadConfigsFromRepositoryAndReturnCorrectEntry() {
    // Arrange
    InitiativeConfig config = new InitiativeConfig();
    config.setInitiativeId("INIT1");

    when(repository.findAll()).thenReturn(List.of(config));

    InitiativeConfigMap map = new InitiativeConfigMap(repository);

    InitiativeConfig result = map.get("INIT1");

    assertNotNull(result);
    assertEquals("INIT1", result.getInitiativeId());
    verify(repository, times(1)).findAll();
  }

  @Test
  void shouldReturnNullWhenInitiativeNotFound() {
    when(repository.findAll()).thenReturn(List.of());

    InitiativeConfigMap map = new InitiativeConfigMap(repository);

    InitiativeConfig result = map.get("UNKNOWN");

    assertNull(result);
  }

  @Test
  void shouldContainInitiativeId() {
    InitiativeConfig config = new InitiativeConfig();
    config.setInitiativeId("INIT2");

    when(repository.findAll()).thenReturn(List.of(config));

    InitiativeConfigMap map = new InitiativeConfigMap(repository);

    assertTrue(map.contains("INIT2"));
  }

  @Test
  void shouldThrowExceptionOnDuplicateKeys() {
    InitiativeConfig c1 = new InitiativeConfig();
    c1.setInitiativeId("INIT1");

    InitiativeConfig c2 = new InitiativeConfig();
    c2.setInitiativeId("INIT1");

    when(repository.findAll()).thenReturn(List.of(c1, c2));

    assertThrows(IllegalStateException.class, () -> new InitiativeConfigMap(repository));
  }
}
