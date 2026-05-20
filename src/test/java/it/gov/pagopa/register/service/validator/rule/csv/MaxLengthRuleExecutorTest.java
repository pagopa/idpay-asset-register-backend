package it.gov.pagopa.register.service.validator.rule.csv;

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
class MaxLengthRuleExecutorTest {

  private MaxLengthRuleExecutor executor;

  @Mock
  private ValidationRule validationRuleMock;

  @Mock
  private CsvRuleContext csvRuleContextMock;

  @BeforeEach
  void setUp() {
    executor = new MaxLengthRuleExecutor();
  }

  @Test
  void testSupports() {
    assertEquals("MAX_LENGTH", executor.supports());
  }

  @Test
  void testEvaluate_False_ValueIsNull() {
    String field = "description_column";

    when(validationRuleMock.getField()).thenReturn(field);
    when(csvRuleContextMock.getValue(field)).thenReturn(null);

    boolean result = executor.evaluate(validationRuleMock, csvRuleContextMock);

    assertFalse(result);
  }

  @Test
  void testEvaluate_True_WithinMaxLength() {
    String field = "description_column";
    String csvValue = "PagoPA";
    String maxAllowedLength = "10";

    when(validationRuleMock.getField()).thenReturn(field);
    when(validationRuleMock.getValue()).thenReturn(maxAllowedLength);
    when(csvRuleContextMock.getValue(field)).thenReturn(csvValue);

    boolean result = executor.evaluate(validationRuleMock, csvRuleContextMock);

    assertTrue(result);
  }

  @Test
  void testEvaluate_True_ExactlyEqualsMaxLength() {
    String field = "description_column";
    String csvValue = "Test";
    String maxAllowedLength = "4";

    when(validationRuleMock.getField()).thenReturn(field);
    when(validationRuleMock.getValue()).thenReturn(maxAllowedLength);
    when(csvRuleContextMock.getValue(field)).thenReturn(csvValue);

    boolean result = executor.evaluate(validationRuleMock, csvRuleContextMock);

    assertTrue(result);
  }

  @Test
  void testEvaluate_False_ExceedsMaxLength() {
    String field = "description_column";
    String csvValue = "QuestoTestoELungo";
    String maxAllowedLength = "5";

    when(validationRuleMock.getField()).thenReturn(field);
    when(validationRuleMock.getValue()).thenReturn(maxAllowedLength);
    when(csvRuleContextMock.getValue(field)).thenReturn(csvValue);

    boolean result = executor.evaluate(validationRuleMock, csvRuleContextMock);

    assertFalse(result);
  }
}
