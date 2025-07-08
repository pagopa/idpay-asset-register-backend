package it.gov.pagopa.register.config;

import it.gov.pagopa.register.utils.EprelValidationRule;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;

@Component
@Data
public class EprelValidationConfig {
  private LinkedHashMap<String, EprelValidationRule> schemas = initialize();

  private static LinkedHashMap<String, EprelValidationRule> initialize() {

    LinkedHashMap<String, EprelValidationRule> schemas = new LinkedHashMap<>();
    schemas.put(ORG_VERIFICATION_STATUS, EprelValidationRules.ORG_VERIFICATION_STATUS_RULE);
    schemas.put(TRADE_MARKER_VERIFICATION_STATUS, EprelValidationRules.TRADE_MARKER_VERIFICATION_STATUS_RULE);
    schemas.put(BLOCKED, EprelValidationRules.BLOCKED_RULE);
    schemas.put(STATUS, EprelValidationRules.STATUS_RULE);
    schemas.put(PRODUCT_GROUP, EprelValidationRules.PRODUCT_GROUP_RULE);
    schemas.put(ENERGY_CLASS, EprelValidationRules.ENERGY_CLASS_RULE);
    return schemas;
  }
}
