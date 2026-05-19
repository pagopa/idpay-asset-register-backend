package it.gov.pagopa.register.service.validator.rule.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import it.gov.pagopa.register.model.initiative.ValidationRule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
  @ParameterizedTest
  @CsvSource({
    "INVALID_CLASS, A4, false",
    "A4, INVALID_REQUIRED, false",
    "A4, A2, true",
    "B, A4, false"
  })
  void evaluate_shouldReturnExpectedResult(
    String actualEnergyClass,
    String requiredEnergyClass,
    boolean expectedResult
  ) {
    Map<String, Object> externalData = new HashMap<>();
    externalData.put("energyClass", actualEnergyClass);

    Map<String, Object> categoryParameters = new HashMap<>();
    categoryParameters.put("minParam", requiredEnergyClass);

    List<String> order = List.of("A4", "A3", "A2", "A1", "B", "C");

    when(externalRuleContextMock.getExternalData()).thenReturn(externalData);
    when(externalRuleContextMock.getCategoryParameters()).thenReturn(categoryParameters);
    when(validationRuleMock.getParam()).thenReturn("minParam");
    when(validationRuleMock.getOrder()).thenReturn(order);

    boolean result = executor.evaluate(validationRuleMock, externalRuleContextMock);

    assertEquals(expectedResult, result);
  }
}
