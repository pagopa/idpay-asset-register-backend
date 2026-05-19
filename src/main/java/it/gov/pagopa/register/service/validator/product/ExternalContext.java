package it.gov.pagopa.register.service.validator.product;

import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import it.gov.pagopa.register.service.validator.external.system.check.ExternalCheckExecutor;
import lombok.Getter;

@Getter
public class ExternalContext {

  private final InitiativeConfig initiativeConfig;
  private final CategoryConfig categoryConfig;
  private final ExternalCheckExecutor externalCheckExecutor;
  private final String category;

  public ExternalContext(
      InitiativeConfig initiativeConfig,
      CategoryConfig categoryConfig,
      ExternalCheckExecutor externalCheckExecutor,
      String category
  ) {
    this.initiativeConfig = initiativeConfig;
    this.categoryConfig = categoryConfig;
    this.externalCheckExecutor = externalCheckExecutor;
    this.category = category;
  }

}
