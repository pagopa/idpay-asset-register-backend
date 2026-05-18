package it.gov.pagopa.register.mapper.product;

import lombok.Data;

import java.util.Map;

@Data
public class MappingContext {

  private final Map<String, Object> externalData;

  public MappingContext(Map<String, Object> externalData) {
    this.externalData = externalData;
  }


}
