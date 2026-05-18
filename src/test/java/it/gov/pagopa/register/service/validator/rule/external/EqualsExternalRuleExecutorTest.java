package it.gov.pagopa.register.service.validator.rule.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import it.gov.pagopa.register.model.initiative.ValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EqualsExternalRuleExecutorTest {

  private EqualsExternalRuleExecutor executor;

  @Mock
  private ValidationRule validationRuleMock;

  @Mock
  private ExternalRuleContext externalRuleContextMock;

  @BeforeEach
  void setUp() {
    executor = new EqualsExternalRuleExecutor();
  }

  @Test
  void testSupports() {
    assertEquals("EQUALS_EXTERNAL", executor.supports());
  }

  @Test
  void testEvaluate_False_ValueIsNull() {
    String field = "external_field";

    when(validationRuleMock.getField()).thenReturn(field);
    when(externalRuleContextMock.getExternalValue(field)).thenReturn(null);

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertFalse(result);
  }

  @Test
  void testEvaluate_True_EqualsIgnoreCase() {
    String field = "external_field";
    String externalValue = "PAGOPA";
    String ruleValue = "pagopa";

    when(validationRuleMock.getField()).thenReturn(field);
    when(validationRuleMock.getValue()).thenReturn(ruleValue);
    when(externalRuleContextMock.getExternalValue(field)).thenReturn(externalValue);

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertTrue(result);
  }

  @Test
  void testEvaluate_False_NotEquals() {
    String field = "external_field";
    String externalValue = "PAGOPA";
    String ruleValue = "different_value";

    when(validationRuleMock.getField()).thenReturn(field);
    when(validationRuleMock.getValue()).thenReturn(ruleValue);
    when(externalRuleContextMock.getExternalValue(field)).thenReturn(externalValue);

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertFalse(result);
  }
}
