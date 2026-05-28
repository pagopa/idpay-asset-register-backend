package it.gov.pagopa.register.connector.initiative;

import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.mapper.operation.InitiativeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortalInitiativeServiceImpl implements PortalInitiativeService {

  private final PortalInitiativeRestClient portalInitiativeRestClient;

  @Override
  public List<InitiativeDTO> getInitiatives(String organizationId) {
    return portalInitiativeRestClient
      .getInitiativeSummary(organizationId, null)
      .getBody()
      .stream()
      .map(InitiativeMapper::toDTO)
      .toList();
  }

  @Override
  public InitiativeDTO getInitiativeDetail(String initiativeId) {
    log.info("[GET_INITIATIVE_DETAIL] - Fetching initiative detail for initiativeId: {}", initiativeId);

    return portalInitiativeRestClient
      .getInitiativeBeneficiaryView(initiativeId)
      .getBody();
  }
}
