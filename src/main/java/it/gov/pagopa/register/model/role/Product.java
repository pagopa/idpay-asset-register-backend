package it.gov.pagopa.register.model.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product")
public class Product {

  /**
   * Identificatore univoco del caricamento
   */
  private String uploadId;

  /**
   * Identificatore univoco dell'utente
   */
  private String userId;

  /**
   * Identificatore univoco dell'organizzazione
   */
  private String organizationId;

  /**
   *
   * Data e ora del caricamento
   */
  private String registrationDate;

  /**
   * Stato del caricamento
   */
  private String status;

  /**
   *
   * Modello del prodotto
   */
  private String modelIdentifier;

  /**
   * Categoria del prodotto Eprel (es. “washingmachines2019")
   */
  private String productGroup;

  /**
   * Categoria generale del prodotto (es. “WASHINGMACHINES")
   */
  private String category;

  /**
   *
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
   * Nome del branch
   */
  private String branchName;

}
