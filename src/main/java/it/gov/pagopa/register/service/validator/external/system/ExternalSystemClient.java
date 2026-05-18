package it.gov.pagopa.register.service.validator.external.system;

import it.gov.pagopa.register.model.initiative.ExternalCheckTemplate;
import org.apache.commons.csv.CSVRecord;

import java.util.Map;

public interface ExternalSystemClient {

  /**
   *    * Tipo di sistema esterno supportato (es. EPREL)
   */
  String supports();

  /**
   * Recupera i dati dal sistema esterno
   */
  Map<String, Object> fetch(
    CSVRecord record,
    ExternalCheckTemplate template
  );
}
