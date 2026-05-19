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
class ProductCountryRuleExecutorTest {

  private ProductCountryRuleExecutor executor;

  @Mock
  private ValidationRule validationRuleMock;

  @Mock
  private CsvRuleContext csvRuleContextMock;

  @BeforeEach
  void setUp() {
    executor = new ProductCountryRuleExecutor();
  }

  @Test
  void testSupports() {
    assertEquals("PRODUCT_COUNTRY", executor.supports());
  }

  @Test
  void testEvaluate_False_ValueIsNull() {
    String field = "country_column";

    when(validationRuleMock.getField()).thenReturn(field);
    when(csvRuleContextMock.getValue(field)).thenReturn(null);

    boolean result = executor.evaluate(validationRuleMock, csvRuleContextMock);

    assertFalse(result);
  }

  @Test
  void testEvaluate_True_ValidIsoCountry() {
    String field = "country_column";
    String validCountry = "IT";

    when(validationRuleMock.getField()).thenReturn(field);
    when(csvRuleContextMock.getValue(field)).thenReturn(validCountry);

    boolean result = executor.evaluate(validationRuleMock, csvRuleContextMock);

    assertTrue(result);
  }

  @Test
  void testEvaluate_False_InvalidIsoCountry() {
    String field = "country_column";
    String invalidCountry = "XYZ";

    when(validationRuleMock.getField()).thenReturn(field);
    when(csvRuleContextMock.getValue(field)).thenReturn(invalidCountry);

    boolean result = executor.evaluate(validationRuleMock, csvRuleContextMock);

    assertFalse(result);
  }
}
