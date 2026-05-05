package it.gov.pagopa.register.service.validator.external.system.eprel;

import it.gov.pagopa.register.configuration.initiative.model.ExternalCheckTemplate;
import it.gov.pagopa.register.connector.eprel.EprelConnector;
import it.gov.pagopa.register.dto.utils.EprelProduct;
import it.gov.pagopa.register.service.validator.external.system.ExternalSystemClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
      CSVRecord record,
      ExternalCheckTemplate template
  ) {

    // Recupero del valore di input dal CSV
    String eprelCode = record.get(template.getInputField());

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
    return mapToExternalData(eprelProduct, template.getRules());
  }

  /**
   * Converte EprelProduct in una mappa generica
   * contenente solo i campi richiesti dalle regole.
   */
  private Map<String, Object> mapToExternalData(
      EprelProduct eprelProduct,
      List<?> rules
  ) {

    Map<String, Object> data = new HashMap<>();

    // NOTA:
    // Estraggo solo i campi che potrebbero servire alle regole.
    // Potrebbero però essere necessari campi extra per la persistenza.

    data.put("status", eprelProduct.getStatus());
    data.put("energyClass", eprelProduct.getEnergyClass());
    data.put("energyClassWash", eprelProduct.getEnergyClassWash());
    data.put("blocked", eprelProduct.getBlocked());
    data.put("productGroup", eprelProduct.getProductGroup());
    data.put("orgVerificationStatus", eprelProduct.getOrgVerificationStatus());
    data.put("trademarkVerificationStatus", eprelProduct.getTrademarkVerificationStatus());

    return data;
  }
}
