package it.gov.pagopa.register.model.initiative;

import it.gov.pagopa.register.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

//TODO apply @NotNull and others to verify initiative config at start up

@NoArgsConstructor
@AllArgsConstructor
@Data
@Document("initiatives_config")
public class InitiativeConfig {

  @Id
  private String initiativeId;
  private String initiativeName;
  /**
   * Configurazione per categoria (lookup diretto via input category)
   */
  private Map<String, CategoryConfig> categories;

  /**
   * Template CSV riusabili (es. EPREL_STANDARD, COOKINGHOBS, ...)
   */
  private Map<String, CsvTemplate> csvTemplates;

  /**
   * Template di controlli esterni (es. EPREL)
   */
  private Map<String, ExternalCheckTemplate> externalCheckTemplates;

  /**
   * Macchina a stati del prodotto per ruolo
   */
  private Map<String, Map<String, List<ProductStatus>>> stateTransitions;

  private List<String> allowedReloadStatuses;

  private String dmDate;


}
