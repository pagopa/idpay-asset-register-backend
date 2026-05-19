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
class TrueFalseExternalRuleExecutorTest {

  private TrueFalseExternalRuleExecutor executor;

  @Mock
  private ValidationRule validationRuleMock;

  @Mock
  private ExternalRuleContext externalRuleContextMock;

  @BeforeEach
  void setUp() {
    executor = new TrueFalseExternalRuleExecutor();
  }

  @Test
  void testSupports() {
    assertEquals("TRUE_FALSE_EXTERNAL", executor.supports());
  }

  @Test
  void testEvaluate_False_ExternalValueNull() {
    String field = "boolean_field";

    when(validationRuleMock.getField()).thenReturn(field);
    when(externalRuleContextMock.getExternalValue(field)).thenReturn(null);

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertFalse(result);
  }

  @Test
  void testEvaluate_True_BooleanMatchTrue() {
    String field = "boolean_field";

    when(validationRuleMock.getField()).thenReturn(field);
    when(validationRuleMock.getValue()).thenReturn("TRUE");
    when(externalRuleContextMock.getExternalValue(field)).thenReturn(true);

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertTrue(result);
  }

  @Test
  void testEvaluate_True_BooleanMatchFalse() {
    String field = "boolean_field";

    when(validationRuleMock.getField()).thenReturn(field);
    when(validationRuleMock.getValue()).thenReturn("false");
    when(externalRuleContextMock.getExternalValue(field)).thenReturn("FALSE");

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertTrue(result);
  }

  @Test
  void testEvaluate_False_BooleanMismatch() {
    String field = "boolean_field";

    when(validationRuleMock.getField()).thenReturn(field);
    when(validationRuleMock.getValue()).thenReturn("true");
    when(externalRuleContextMock.getExternalValue(field)).thenReturn(false);

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertFalse(result);
  }

  @Test
  void testEvaluate_True_StringFallbackMatch() {
    String field = "string_field";

    when(validationRuleMock.getField()).thenReturn(field);
    when(validationRuleMock.getValue()).thenReturn("PENDING");
    when(externalRuleContextMock.getExternalValue(field)).thenReturn("pending");

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertTrue(result);
  }

  @Test
  void testEvaluate_False_StringFallbackMismatch() {
    String field = "string_field";

    when(validationRuleMock.getField()).thenReturn(field);
    when(validationRuleMock.getValue()).thenReturn("PENDING");
    when(externalRuleContextMock.getExternalValue(field)).thenReturn("COMPLETED");

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertFalse(result);
  }
}
