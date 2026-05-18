package it.gov.pagopa.register.service.validator.rule.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import it.gov.pagopa.register.model.initiative.ValidationRule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MinEnergyClassRuleExecutorTest {

  private MinEnergyClassRuleExecutor executor;

  @Mock
  private ValidationRule validationRuleMock;

  @Mock
  private ExternalRuleContext externalRuleContextMock;

  @BeforeEach
  void setUp() {
    executor = new MinEnergyClassRuleExecutor();
  }

  @Test
  void testSupports() {
    assertEquals("MIN_ENERGY_CLASS", executor.supports());
  }

  @Test
  void testEvaluate_False_ActualObjNull() {
    Map<String, Object> externalData = new HashMap<>();

    when(externalRuleContextMock.getExternalData()).thenReturn(externalData);

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertFalse(result);
  }

  @Test
  void testEvaluate_False_ActualIndexNotFound() {
    Map<String, Object> externalData = new HashMap<>();
    externalData.put("energyClass", "INVALID_CLASS");

    Map<String, Object> categoryParameters = new HashMap<>();
    categoryParameters.put("minParam", "A4");

    List<String> order = List.of("A4", "A3", "A2", "A1", "B", "C");

    when(externalRuleContextMock.getExternalData()).thenReturn(externalData);
    when(externalRuleContextMock.getCategoryParameters()).thenReturn(categoryParameters);
    when(validationRuleMock.getParam()).thenReturn("minParam");
    when(validationRuleMock.getOrder()).thenReturn(order);

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertFalse(result);
  }

  @Test
  void testEvaluate_False_RequiredIndexNotFound() {
    Map<String, Object> externalData = new HashMap<>();
    externalData.put("energyClass", "A4");

    Map<String, Object> categoryParameters = new HashMap<>();
    categoryParameters.put("minParam", "INVALID_REQUIRED");

    List<String> order = List.of("A4", "A3", "A2", "A1", "B", "C");

    when(externalRuleContextMock.getExternalData()).thenReturn(externalData);
    when(externalRuleContextMock.getCategoryParameters()).thenReturn(categoryParameters);
    when(validationRuleMock.getParam()).thenReturn("minParam");
    when(validationRuleMock.getOrder()).thenReturn(order);

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertFalse(result);
  }

  @Test
  void testEvaluate_True_ValidAndLowerOrEqualIndex() {
    Map<String, Object> externalData = new HashMap<>();
    externalData.put("energyClass", "A4");

    Map<String, Object> categoryParameters = new HashMap<>();
    categoryParameters.put("minParam", "A2");

    List<String> order = List.of("A4", "A3", "A2", "A1", "B", "C");

    when(externalRuleContextMock.getExternalData()).thenReturn(externalData);
    when(externalRuleContextMock.getCategoryParameters()).thenReturn(categoryParameters);
    when(validationRuleMock.getParam()).thenReturn("minParam");
    when(validationRuleMock.getOrder()).thenReturn(order);

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertTrue(result);
  }

  @Test
  void testEvaluate_False_HigherIndex() {
    Map<String, Object> externalData = new HashMap<>();
    externalData.put("energyClass", "B");

    Map<String, Object> categoryParameters = new HashMap<>();
    categoryParameters.put("minParam", "A4");

    List<String> order = List.of("A4", "A3", "A2", "A1", "B", "C");

    when(externalRuleContextMock.getExternalData()).thenReturn(externalData);
    when(externalRuleContextMock.getCategoryParameters()).thenReturn(categoryParameters);
    when(validationRuleMock.getParam()).thenReturn("minParam");
    when(validationRuleMock.getOrder()).thenReturn(order);

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertFalse(result);
  }
}
