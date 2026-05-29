package it.gov.pagopa.register.connector.initiative;

import it.gov.pagopa.register.dto.operation.InitiativeDTO;

import java.util.List;

public interface PortalInitiativeService {

  List<InitiativeDTO> getInitiatives(String organizationId);
}
