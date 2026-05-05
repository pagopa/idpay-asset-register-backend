package it.gov.pagopa.register.configuration.initiative.model;

import lombok.Data;

import java.util.List;

@Data
public class ValidationRule {

  /**
   * Tipo di regola
   * (EQUALS, MIN_ENERGY_CLASS, PRODUCT_GROUP_MATCH_CATEGORY, ecc.)
   */
  private String type;

  /**
   * Campo della risposta esterna a cui applicare la regola
   * es. status, energyClass, productGroup
   */
  private String field;

  /**
   * Valore statico per confronti diretti
   * (usato da EQUALS)
   */
  private String value;

  /**
   * Nome del parametro dinamico definito nella CategoryConfig
   * es. minEnergyClass
   */
  private String param;

  /**
   * Ordine di confronto per regole ordinali
   * es. classi energetiche
   */
  private List<String> order;
}
