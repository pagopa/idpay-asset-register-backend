package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.dto.operation.ProducerDTO;
import it.gov.pagopa.register.dto.operation.ProducersResponseDTO;
import it.gov.pagopa.register.mapper.operation.ProducerMapper;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProducersService {

  private final ProducersInitiativeRepository producersInitiativeRepository;

  public ProducersResponseDTO getProducersByInitiative(String initiativeId, Pageable pageable, boolean paged) {
    log.info("[GET_PRODUCERS_BY_INITIATIVE] - Fetching producers for initiativeId: {}", initiativeId);

    Pageable executedPageable = paged
      ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort())
      : Pageable.unpaged();

    Page<ProducersInitiative> producersPage =
      producersInitiativeRepository.findByInitiativeId(initiativeId, executedPageable);
    Page<ProducerDTO> producerDTOPage = producersPage.map(ProducerMapper::toDTO);

    log.info("[GET_PRODUCERS_BY_INITIATIVE] - Fetched {} producers for initiativeId: {}",
      producerDTOPage.getTotalElements(), initiativeId);

    return ProducersResponseDTO.builder()
      .content(producerDTOPage.getContent())
      .pageNo(paged ? producerDTOPage.getNumber() : 0)
      .pageSize(paged ? producerDTOPage.getSize() : producerDTOPage.getTotalElements())
      .totalElements(producerDTOPage.getTotalElements())
      .totalPages(paged ? producerDTOPage.getTotalPages() : 1)
      .build();
  }
}
