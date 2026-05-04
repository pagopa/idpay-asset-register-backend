package it.gov.pagopa.register.configuration.initiative;

import lombok.Data;

import java.util.Map;

@Data
public class CategoryExternalCheck {

  /**
   * es. "EPREL"
   */
  private String name;

  /**
   * Parametri specifici per la categoria
   * es. minEnergyClass=A
   */
  private Map<String, Object> parameters;
}

