package it.gov.pagopa.register.service.validator.external.system.eprel;

import it.gov.pagopa.register.model.initiative.ExternalCheckTemplate;
import it.gov.pagopa.register.connector.eprel.EprelConnector;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.service.validator.external.system.ExternalSystemClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.EprelField.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class EprelExternalSystemClient implements ExternalSystemClient {

  private static final String SYSTEM_TYPE = "EPREL";

  private final EprelConnector eprelConnector;

  @Override
  public String supports() {
    return SYSTEM_TYPE;
  }

  @Override
  public Map<String, Object> fetch(
      CSVRecord csvRecord,
      ExternalCheckTemplate template
  ) {

    // Recupero del valore di input dal CSV
    String eprelCode = csvRecord.get(template.getInputField());

    if (!StringUtils.hasText(eprelCode)) {
      throw new IllegalArgumentException(
          "Codice EPREL assente nel campo: " + template.getInputField()
      );
    }

    // Chiamata a EPREL
    EprelProduct eprelProduct = eprelConnector.callEprel(eprelCode);

    if (eprelProduct == null) {
      throw new IllegalStateException(
          "EPREL non ha restituito dati per il codice: " + eprelCode
      );
    }

    log.debug("[EPREL] Dati ricevuti per codice {}: {}", eprelCode, eprelProduct);

    // Conversione verso struttura generica
    return mapToExternalData(eprelProduct);
  }

  /**
   * Converte EprelProduct in una mappa generica
   * contenente solo i campi richiesti dalle regole.
   */
  private Map<String, Object> mapToExternalData(
      EprelProduct eprelProduct
  ) {

    Map<String, Object> data = new HashMap<>();

    data.put(STATUS, eprelProduct.getStatus());
    data.put(ENERGY_CLASS, eprelProduct.getEnergyClass());
    data.put(ENERGY_CLASS_WASH, eprelProduct.getEnergyClassWash());
    data.put(BLOCKED, eprelProduct.getBlocked());
    data.put(PRODUCT_GROUP, eprelProduct.getProductGroup());
    data.put(ORG_VERIFICATION_STATUS, eprelProduct.getOrgVerificationStatus());
    data.put(TRADEMARK_VERIFICATION_STATUS, eprelProduct.getTrademarkVerificationStatus());
    data.put(SUPPLIER_OR_TRADEMARK, eprelProduct.getSupplierOrTrademark());
    data.put(MODEL_IDENTIFIER, eprelProduct.getModelIdentifier());
    data.put(RATED_CAPACITY, eprelProduct.getRatedCapacity());
    data.put(RATED_CAPACITY_WASH, eprelProduct.getRatedCapacityWash());
    data.put(CAVITIES, eprelProduct.getCavities());
    data.put(TOTAL_VOLUME, eprelProduct.getTotalVolume());
    return data;
  }
}
