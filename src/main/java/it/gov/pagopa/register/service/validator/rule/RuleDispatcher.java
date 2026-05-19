package it.gov.pagopa.register.service.validator.rule;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RuleDispatcher {

  private final Map<String, RuleExecutor> executors;

  public RuleDispatcher(List<RuleExecutor> executors) {
    this.executors = executors.stream()
        .collect(Collectors.toMap(RuleExecutor::supports, e -> e));
  }

  public RuleExecutor resolve(String type) {
    return executors.get(type);
  }
}
