package it.gov.pagopa.register.configuration.initiative;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CategoryConfig {

  /**
   * Riferimento a un CsvTemplate
   */
  private String csvTemplate;

  /**
   * Nome del campo CSV che rappresenta l'identificativo primario
   * (a livello di input)
   * es: CODE_GTIN_EAN, SERIAL_NUMBER
   */
  private String inputIdentifierField;

  /**
   * External checks da applicare (per nome template)
   * es: "EPREL" -> { minEnergyClass: "A" }
   */
  private List<CategoryExternalCheck> externalChecks;


  /**
   * Identificatore del mapper di dominio
   * es: "EPREL" , "DECODE"
   */
   private String productMapper;
}
