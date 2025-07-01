package it.gov.pagopa.register.model.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Document(collection = "product")

public class Product {

  @Id
  private String id;

  /**
   * Identificatore univoco del caricamento
   */
  private String productFileId;



  /**
   * Identificatore univoco dell'organizzazione
   */
  private String organizationId;

  /**
   *
   * Data e ora del caricamento
   */
  private LocalDateTime registrationDate;

  /**
   * Stato del caricamento
   */
  private String status;

  /**
   *
   * Modello del prodotto
   */
  private String model;

  /**
   * Categoria del prodotto Eprel (es. “washingmachines2019")
   */
  private String productGroup;

  /**
   * Categoria generale del prodotto (es. “WASHINGMACHINES")
   */
  private String category;

  /**
   * Marca del prodotto
   */
  private String brand;

  /**
   * Identificativo univoco del prodotto nel sistema EPREL (Registration Number)
   */
  private String eprelCode;

  /**
   * Codice commerciale del prodotto - chiave univoca
   */
  private String gtinCode;

  /**
   * Identificativo del prodotto interno all’azienda (SKU)
   */
  private String productCode;

  /**
   * Nazione di produzione (codice ISO 3166-1 Alpha-2)
   */
  private String countryOfProduction;

  /**
   * Classe energetica del prodotto
   */
  private String energyClass;

  /**
   * Link di dettaglio prodotto
   */
  private String linkEprel;

}
