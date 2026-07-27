package it.gov.pagopa.register.service.validator.external.system;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExternalSystemClientDispatcher {

  /**
   * Mappa: tipo sistema esterno -> client
   * es. "EPREL" -> EprelExternalSystemClient
   */
  private final Map<String, ExternalSystemClient> clientsByType;

  public ExternalSystemClientDispatcher(
      List<ExternalSystemClient> clients
  ) {
    this.clientsByType = clients.stream()
        .collect(Collectors.toUnmodifiableMap(
            ExternalSystemClient::supports,
            Function.identity()
        ));
  }

  /**
   * Restituisce il client per il tipo richiesto.
   * Ritorna null se non esiste alcun client compatibile.
   */
  public ExternalSystemClient resolve(String type) {
    return clientsByType.get(type);
  }
}
