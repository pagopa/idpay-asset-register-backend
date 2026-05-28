package it.gov.pagopa.register.mapper.operation;

import it.gov.pagopa.register.dto.operation.ProducerDTO;
import it.gov.pagopa.register.model.operation.ProducersInitiative;

public class ProducerMapper {

  private ProducerMapper() {}

  public static ProducerDTO toDTO(ProducersInitiative entity) {
    return ProducerDTO.builder()
      .producerName(entity.getProducerName())
      .createdAt(entity.getCreatedAt())
      .updatedAt(entity.getUpdatedAt())
      .build();
  }
}
