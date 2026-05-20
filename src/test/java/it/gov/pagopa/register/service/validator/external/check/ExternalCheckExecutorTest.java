package it.gov.pagopa.register.service.validator.external.check;

import it.gov.pagopa.register.model.initiative.ExternalCheckTemplate;
import it.gov.pagopa.register.model.initiative.ValidationRule;
import it.gov.pagopa.register.service.validator.external.system.ExternalSystemClient;
import it.gov.pagopa.register.service.validator.external.system.ExternalSystemClientDispatcher;
import it.gov.pagopa.register.service.validator.external.system.check.ExternalCheckExecutor;
import it.gov.pagopa.register.service.validator.external.system.check.ExternalCheckResult;
import it.gov.pagopa.register.service.validator.rule.RuleDispatcher;
import it.gov.pagopa.register.service.validator.rule.RuleExecutor;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ExternalCheckExecutor.class)
class ExternalCheckExecutorTest {

  @MockitoBean
  private ExternalSystemClientDispatcher systemClientDispatcher;

  @MockitoBean
  private RuleDispatcher ruleDispatcher;

  @MockitoBean
  private ExternalSystemClient systemClient;

  @MockitoBean
  private RuleExecutor ruleExecutor;

  @Autowired
  private ExternalCheckExecutor executor;

  @Test
  void shouldReturnKoWhenSystemNotSupported() {

    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);
    when(template.getType()).thenReturn("TYPE");

    when(systemClientDispatcher.resolve("TYPE")).thenReturn(null);

    ExternalCheckResult result = executor.execute(
      mock(CSVRecord.class),
      template,
      Map.of(),
      "CAT"
    );

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("Sistema esterno non supportato"));
  }

  @Test
  void shouldReturnKoWhenFetchThrowsException() {

    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);
    when(template.getType()).thenReturn("TYPE");

    when(systemClientDispatcher.resolve("TYPE")).thenReturn(systemClient);

    when(systemClient.fetch(any(), any()))
      .thenThrow(new RuntimeException("boom"));

    ExternalCheckResult result = executor.execute(
      mock(CSVRecord.class),
      template,
      Map.of(),
      "CAT"
    );

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("Errore durante la chiamata"));
  }

  @Test
  void shouldReturnKoWhenExternalDataEmpty() {

    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);
    when(template.getType()).thenReturn("TYPE");

    when(systemClientDispatcher.resolve("TYPE")).thenReturn(systemClient);

    when(systemClient.fetch(any(), any()))
      .thenReturn(Map.of());

    ExternalCheckResult result = executor.execute(
      mock(CSVRecord.class),
      template,
      Map.of(),
      "CAT"
    );

    assertFalse(result.isValid());
  }

  @Test
  void shouldReturnKoWhenRuleExecutorNotFound() {

    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);
    ValidationRule rule = mock(ValidationRule.class);

    when(template.getType()).thenReturn("TYPE");
    when(template.getRules()).thenReturn(List.of(rule));

    when(systemClientDispatcher.resolve("TYPE")).thenReturn(systemClient);
    when(systemClient.fetch(any(), any())).thenReturn(Map.of("k", "v"));

    when(rule.getKey()).thenReturn("RULE");
    when(ruleDispatcher.resolve("RULE")).thenReturn(null);

    ExternalCheckResult result = executor.execute(
      mock(CSVRecord.class),
      template,
      Map.of(),
      "CAT"
    );

    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("Errore nella configurazione"));
  }

  @Test
  void shouldReturnKoWhenRuleFails() {

    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);
    ValidationRule rule = mock(ValidationRule.class);

    when(template.getType()).thenReturn("TYPE");
    when(template.getRules()).thenReturn(List.of(rule));

    when(systemClientDispatcher.resolve("TYPE")).thenReturn(systemClient);
    when(systemClient.fetch(any(), any())).thenReturn(Map.of("k", "v"));

    when(rule.getKey()).thenReturn("RULE");
    when(rule.getErrorKey()).thenReturn("ERROR_GTIN_EAN");

    when(ruleDispatcher.resolve("RULE")).thenReturn(ruleExecutor);
    when(ruleExecutor.evaluate(any(), any())).thenReturn(false);

    ExternalCheckResult result = executor.execute(
      mock(CSVRecord.class),
      template,
      Map.of(),
      "CATEGORY"
    );
    assertFalse(result.isValid());
  }

  @Test
  void shouldReturnOkWhenAllValid() {

    ExternalCheckTemplate template = mock(ExternalCheckTemplate.class);
    ValidationRule rule = mock(ValidationRule.class);

    when(template.getType()).thenReturn("TYPE");
    when(template.getRules()).thenReturn(List.of(rule));

    when(systemClientDispatcher.resolve("TYPE")).thenReturn(systemClient);
    when(systemClient.fetch(any(), any())).thenReturn(Map.of("k", "v"));

    when(rule.getKey()).thenReturn("RULE");

    when(ruleDispatcher.resolve("RULE")).thenReturn(ruleExecutor);
    when(ruleExecutor.evaluate(any(), any())).thenReturn(true);

    ExternalCheckResult result = executor.execute(
      mock(CSVRecord.class),
      template,
      Map.of("param", "value"),
      "CATEGORY"
    );

    assertTrue(result.isValid());
    assertEquals("v", result.getExternalData().get("k"));
  }
}
