package it.gov.pagopa.register.configuration.initiative.mapper;

import java.util.Map;

public class MappingContext {

  private final Map<String, Object> externalData;

  public MappingContext(Map<String, Object> externalData) {
    this.externalData = externalData;
  }

  public <T> T getExternalData(String key, Class<T> clazz) {
    return clazz.cast(externalData.get(key));
  }
}
