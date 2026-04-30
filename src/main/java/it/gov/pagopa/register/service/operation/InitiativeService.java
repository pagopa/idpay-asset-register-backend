package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.initiative.PortalInitiativeService;
import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.mapper.operation.InitiativeMapper;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InitiativeService {

  private final ProducersInitiativeRepository repository;
  private final PortalInitiativeService portalInitiativeService;

  public InitiativeService(ProducersInitiativeRepository repository, PortalInitiativeService portalInitiativeService) {
    this.repository = repository;
    this.portalInitiativeService = portalInitiativeService;
  }

  public List<InitiativeDTO> getInitiatives(String role, String organizationId) {

    if(role.equals(UserRole.OPERATORE.getRole()))
      return repository.findByProducersId(organizationId)
        .stream()
        .filter(ProducersInitiative::getEnabled)
        .map(InitiativeMapper::toDTO)
        .toList();

    return portalInitiativeService.getInitiatives(organizationId);
  }
}
