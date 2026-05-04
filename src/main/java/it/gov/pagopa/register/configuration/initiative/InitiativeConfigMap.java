package it.gov.pagopa.register.configuration.initiative;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InitiativeConfigMap {

  private final Map<String, InitiativeConfig> configsByInitiativeId;

  public InitiativeConfigMap(List<InitiativeConfig> initiativeConfigs) {
    this.configsByInitiativeId =
      initiativeConfigs.stream()
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
