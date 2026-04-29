package it.gov.pagopa.register.mapper.operation;

import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.InitiativeSummaryDTO;
import it.gov.pagopa.register.model.operation.ProducersInitiative;

public class InitiativeMapper {

  public InitiativeDTO toDTO(ProducersInitiative entity) {
    return InitiativeDTO.builder()
      .initiativeId(entity.getInitiativeId())
      .initiativeName(entity.getInitiativeName())
      .status(entity.getInitiativeStatus())
      .startDate(entity.getInitiativeStartDate())
      .endDate(entity.getInitiativeEndDate())
      .serviceId(entity.getInitiativeServiceId())
      .organizationName(entity.getInitiativeOrganizationName())
      .enabled(entity.getEnabled())
      .build();
  }

  public InitiativeDTO toDTO(InitiativeSummaryDTO dto) {
    return InitiativeDTO.builder()
      .initiativeId(dto.getInitiativeId())
      .initiativeName(dto.getInitiativeName())
      .status(InitiativeStatus.valueOf(dto.getStatus()))
      .startDate(dto.getCreationDate().toLocalDate())
      .build();
  }
}
