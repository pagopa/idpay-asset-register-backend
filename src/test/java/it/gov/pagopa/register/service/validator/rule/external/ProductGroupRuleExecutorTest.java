package it.gov.pagopa.register.service.validator.rule.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import it.gov.pagopa.register.model.initiative.ValidationRule;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductGroupRuleExecutorTest {

  private ProductGroupRuleExecutor executor;

  @Mock
  private ValidationRule validationRuleMock;

  @Mock
  private ExternalRuleContext externalRuleContextMock;

  @BeforeEach
  void setUp() {
    executor = new ProductGroupRuleExecutor();
  }

  @Test
  void testSupports() {
    assertEquals("PRODUCT_GROUP_MATCH", executor.supports());
  }

  @Test
  void testEvaluate_False_ProductGroupNull() {
    Map<String, Object> externalData = new HashMap<>();

    when(externalRuleContextMock.getExternalData()).thenReturn(externalData);

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertFalse(result);
  }

  @Test
  void testEvaluate_True_StartsWithCategoryIgnoreCase() {
    Map<String, Object> externalData = new HashMap<>();
    externalData.put("productGroup", "WELFARE_BONUS_2026");

    when(externalRuleContextMock.getExternalData()).thenReturn(externalData);
    when(externalRuleContextMock.getCategory()).thenReturn("welfare");

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertTrue(result);
  }

  @Test
  void testEvaluate_False_DoesNotStartWithCategory() {
    Map<String, Object> externalData = new HashMap<>();
    externalData.put("productGroup", "ECOLOGY_INCENTIVE");

    when(externalRuleContextMock.getExternalData()).thenReturn(externalData);
    when(externalRuleContextMock.getCategory()).thenReturn("welfare");

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertFalse(result);
  }
}
