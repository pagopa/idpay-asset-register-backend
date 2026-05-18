package it.gov.pagopa.register.model.initiative;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ValidationRule {

  /**
   * Chiave della regola
   * (EQUALS, MIN_ENERGY_CLASS, PRODUCT_GROUP_MATCH_CATEGORY, ecc.)
   */
  private String key;

  /**
   * Chiave del messaggio d'errore
   */
  private String errorKey;

  /**
   * Campo della risposta esterna a cui applicare la regola o nome header csv
   * es. status, energyClass, productGroup, Modello, Marca
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

  @Builder.Default
  private List<String> order = new ArrayList<>();

}
