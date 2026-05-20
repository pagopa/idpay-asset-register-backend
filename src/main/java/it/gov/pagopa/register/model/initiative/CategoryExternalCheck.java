package it.gov.pagopa.register.model.initiative;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CategoryExternalCheck {

  /**
   * es. "EPREL"
   */
  private String key;

  /**
   * Parametri specifici per la categoria
   * es. minEnergyClass=A
   */
  private Map<String, Object> parameters;
}

