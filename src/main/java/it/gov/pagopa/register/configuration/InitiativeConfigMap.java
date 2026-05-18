package it.gov.pagopa.register.configuration;

import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import it.gov.pagopa.register.repository.initiative.InitiativeRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InitiativeConfigMap {

  private final Map<String, InitiativeConfig> configsByInitiativeId;

  public InitiativeConfigMap(InitiativeRepository repository) {
    this.configsByInitiativeId =
      repository.findAll().stream()
        .collect(Collectors.toUnmodifiableMap(
          InitiativeConfig::getInitiativeId,
          Function.identity()
        ));
  }

  public InitiativeConfig get(String initiativeId) {
    return configsByInitiativeId.get(initiativeId);
  }

  public boolean contains(String initiativeId) {
    return configsByInitiativeId.containsKey(initiativeId);
  }
}
