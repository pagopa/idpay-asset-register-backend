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
class RegexRuleExecutorTest {

  private RegexRuleExecutor executor;

  @Mock
  private ValidationRule validationRuleMock;

  @Mock
  private CsvRuleContext csvRuleContextMock;

  @BeforeEach
  void setUp() {
    executor = new RegexRuleExecutor();
  }

  @Test
  void testSupports() {
    assertEquals("REGEX", executor.supports());
  }

  @Test
  void testEvaluate_False_ValueIsNull() {
    String field = "email_column";

    when(validationRuleMock.getField()).thenReturn(field);
    when(csvRuleContextMock.getValue(field)).thenReturn(null);

    boolean result = executor.evaluate(validationRuleMock, csvRuleContextMock);

    assertFalse(result);
  }

  @Test
  void testEvaluate_True_MatchesRegex() {
    String field = "numeric_column";
    String csvValue = "12345";
    String regexPattern = "^[0-9]+$";

    when(validationRuleMock.getField()).thenReturn(field);
    when(validationRuleMock.getValue()).thenReturn(regexPattern);
    when(csvRuleContextMock.getValue(field)).thenReturn(csvValue);

    boolean result = executor.evaluate(validationRuleMock, csvRuleContextMock);

    assertTrue(result);
  }

  @Test
  void testEvaluate_False_DoesNotMatchRegex() {
    String field = "numeric_column";
    String csvValue = "123abc45";
    String regexPattern = "^[0-9]+$";

    when(validationRuleMock.getField()).thenReturn(field);
    when(validationRuleMock.getValue()).thenReturn(regexPattern);
    when(csvRuleContextMock.getValue(field)).thenReturn(csvValue);

    boolean result = executor.evaluate(validationRuleMock, csvRuleContextMock);

    assertFalse(result);
  }
}
