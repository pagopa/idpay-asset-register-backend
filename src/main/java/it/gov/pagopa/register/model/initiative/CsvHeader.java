package it.gov.pagopa.register.model.initiative;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CsvHeader {

  /**
   * Nome della colonna nel file CSV (es. "Marca")
   */
  private String name;

}
