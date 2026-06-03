package it.gov.pagopa.register.dto.operation;

import lombok.Data;

import java.util.List;

@Data
public class ProducerInitiativeImportRequestDTO {
  private List<ProducerInitiativeRequestDTO> producers;
}
