package it.gov.pagopa.register.service.validator.rule.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.model.initiative.ValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EqualsCategoryRuleExecutorTest {

  private EqualsCategoryRuleExecutor executor;

  @Mock
  private ValidationRule validationRuleMock;

  @Mock
  private CsvRuleContext csvRuleContextMock;

  @BeforeEach
  void setUp() {
    executor = new EqualsCategoryRuleExecutor();
  }

  @Test
  void testSupports() {
    assertEquals("EQUALS_CATEGORY", executor.supports());
  }

  @Test
  void testEvaluate_True() {
    var entry = AssetRegisterConstants.CATEGORIES_TO_IT_S.entrySet().iterator().next();
    String realKey = entry.getKey();
    String realValue = entry.getValue();

    System.out.println("DEBUG TEST - Chiave reale: " + realKey + " | Valore reale: " + realValue);

    String field = "category_column";

    when(validationRuleMock.getField()).thenReturn(field);

    when(csvRuleContextMock.getValue(field)).thenReturn(realValue);

    when(csvRuleContextMock.getCategory()).thenReturn(realKey);

    boolean result = executor.evaluate(validationRuleMock, csvRuleContextMock);

    assertTrue(result, "Il confronto è fallito! Controlla i valori estratti dalla mappa nei log.");
  }

  @Test
  void testEvaluate_False_DifferentCategory() {
    String field = "category_column";

    String contextCategory = "CHIAVE_REALE";
    String csvDifferentCategoryValue = "Categoria Totalmente Inventata Che Non Matcherà";

    when(validationRuleMock.getField()).thenReturn(field);
    when(csvRuleContextMock.getValue(field)).thenReturn(csvDifferentCategoryValue);
    when(csvRuleContextMock.getCategory()).thenReturn(contextCategory);

    boolean result = executor.evaluate(validationRuleMock, csvRuleContextMock);

    assertFalse(result);
  }

  @Test
  void testEvaluate_False_CsvCategoryNull() {
    String field = "category_column";
    String contextCategory = "WELFARE_KEY";

    when(validationRuleMock.getField()).thenReturn(field);
    when(csvRuleContextMock.getValue(field)).thenReturn(null);
    when(csvRuleContextMock.getCategory()).thenReturn(contextCategory);

    boolean result = executor.evaluate(validationRuleMock, csvRuleContextMock);

    assertFalse(result);
  }
}
