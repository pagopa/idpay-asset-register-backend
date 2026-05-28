package it.gov.pagopa.register.connector.initiative;

import it.gov.pagopa.register.dto.operation.InitiativeSummaryDTO;
import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
  name = "${app.rest-client.portal-initiative.service.name}",
  url = "${app.rest-client.portal-initiative.service.base-url}"
)
public interface PortalInitiativeRestClient {

  /**
   * Returns the list of initiatives names for a specific organization
   *
   * @param organizationId organization identifier
   * @param role optional role filter
   * @return list of InitiativeSummaryDTO
   */
  @GetMapping(
    value = "/idpay/organization/{organizationId}/initiative/summary",
    produces = "application/json"
  )
  ResponseEntity<List<InitiativeSummaryDTO>> getInitiativeSummary(
    @PathVariable("organizationId") String organizationId,
    @RequestParam(value = "role", required = false) String role
  );

  @GetMapping(
    value = "/idpay/organization/{organizationId}/initiative/{initiativeId}",
    produces = "application/json"
  )
  ResponseEntity<InitiativeDTO> getInitiativeDetail(
    @PathVariable("organizationId") String organizationId,
    @PathVariable("initiativeId") String initiativeId,
    @RequestParam(value = "role", required = false) String role
  );

}
