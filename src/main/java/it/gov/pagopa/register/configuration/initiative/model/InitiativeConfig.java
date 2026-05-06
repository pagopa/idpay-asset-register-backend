package it.gov.pagopa.register.configuration.initiative.model;

import it.gov.pagopa.register.enums.ProductStatus;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class InitiativeConfig {

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
  private Map<String, Map<ProductStatus, List<ProductStatus>>> stateTransitions;

  private List<String> allowedReloadStatuses;
  public boolean isTransitionAllowed(
    String role,
    ProductStatus currentStatus,
    ProductStatus targetStatus
  ) {

    if (stateTransitions == null) {
      return false;
    }

    Map<ProductStatus, List<ProductStatus>> roleTransitions =
      stateTransitions.get(role);

    if (roleTransitions == null) {
      return false;
    }

    List<ProductStatus> allowedInitialStates =
      roleTransitions.get(targetStatus);

    if (allowedInitialStates == null) {
      return false;
    }

    return allowedInitialStates.contains(currentStatus);
  }

}
