package it.gov.pagopa.register.mapper.operation;

import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.InitiativeSummaryDTO;
import it.gov.pagopa.register.model.operation.ProducersInitiative;

public class InitiativeMapper {

  private InitiativeMapper(){}

  public static InitiativeDTO toDTO(ProducersInitiative entity) {
    return InitiativeDTO.builder()
      .initiativeId(entity.getInitiativeId())
      .initiativeName(entity.getInitiativeName())
      .status(entity.getInitiativeStatus())
      .startDate(entity.getInitiativeStartDate().toLocalDate())
      .endDate(entity.getInitiativeEndDate().toLocalDate())
      .serviceId(entity.getInitiativeServiceId())
      .organizationName(entity.getInitiativeOrganizationName())
      .enabled(entity.getEnabled())
      .build();
  }

  public static InitiativeDTO toDTO(InitiativeSummaryDTO dto) {
    return InitiativeDTO.builder()
      .initiativeId(dto.getInitiativeId())
      .initiativeName(dto.getInitiativeName())
      .organizationName(dto.getOrganizationName())
      .status(InitiativeStatus.valueOf(dto.getStatus()))
      .startDate(dto.getStartDate())
      .endDate(dto.getEndDate())
      .serviceId(dto.getServiceId())
      .enabled(true)
      .build();
  }
}
