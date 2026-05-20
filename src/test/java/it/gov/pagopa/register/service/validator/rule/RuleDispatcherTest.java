package it.gov.pagopa.register.service.validator.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleDispatcherTest {

  private RuleDispatcher ruleDispatcher;

  @Mock
  private RuleExecutor executorMock1;

  @Mock
  private RuleExecutor executorMock2;

  @BeforeEach
  void setUp() {
    when(executorMock1.supports()).thenReturn("RULE_TYPE_1");
    when(executorMock2.supports()).thenReturn("RULE_TYPE_2");

    List<RuleExecutor> executors = List.of(executorMock1, executorMock2);
    ruleDispatcher = new RuleDispatcher(executors);
  }

  @Test
  void testResolve_Success() {
    RuleExecutor result = ruleDispatcher.resolve("RULE_TYPE_1");

    assertNotNull(result);
    assertEquals(executorMock1, result);
  }

  @Test
  void testResolve_NotFound() {
    RuleExecutor result = ruleDispatcher.resolve("UNKNOWN_TYPE");

    assertNull(result);
  }
}
