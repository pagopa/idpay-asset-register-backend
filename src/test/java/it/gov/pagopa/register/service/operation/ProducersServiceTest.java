package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.dto.operation.ProducersResponseDTO;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProducersServiceTest {

  @Mock
  private ProducersInitiativeRepository producersInitiativeRepository;

  @InjectMocks
  private ProducersService producersService;

  @Test
  void getProducersByInitiative_shouldReturnPagedProducerData() {
    String initiativeId = "initiative-1";
    Pageable pageable = PageRequest.of(0, 20);
    LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 30);
    LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 2, 11, 45);

    ProducersInitiative producer = ProducersInitiative.builder()
      .producerName("Producer 1")
      .createdAt(createdAt)
      .updatedAt(updatedAt)
      .build();

    when(producersInitiativeRepository.findByInitiativeId(initiativeId, pageable))
      .thenReturn(new PageImpl<>(List.of(producer), pageable, 1));

    ProducersResponseDTO result = producersService.getProducersByInitiative(initiativeId, pageable);

    assertEquals(1, result.getContent().size());
    assertEquals("Producer 1", result.getContent().getFirst().getProducerName());
    assertEquals(createdAt, result.getContent().getFirst().getCreatedAt());
    assertEquals(updatedAt, result.getContent().getFirst().getUpdatedAt());
    assertEquals(0, result.getPageNo());
    assertEquals(20, result.getPageSize());
    assertEquals(1, result.getTotalElements());
    assertEquals(1, result.getTotalPages());
    verify(producersInitiativeRepository).findByInitiativeId(initiativeId, pageable);
  }

  @Test
  void getProducersByInitiative_shouldRemoveSortBeforeQueryingMongo() {
    String initiativeId = "initiative-1";
    Pageable pageable = PageRequest.of(2, 5, Sort.by("producerName").ascending());

    when(producersInitiativeRepository.findByInitiativeId(org.mockito.ArgumentMatchers.eq(initiativeId), org.mockito.ArgumentMatchers.any(Pageable.class)))
      .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

    producersService.getProducersByInitiative(initiativeId, pageable);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(producersInitiativeRepository).findByInitiativeId(org.mockito.ArgumentMatchers.eq(initiativeId), pageableCaptor.capture());
    assertEquals(2, pageableCaptor.getValue().getPageNumber());
    assertEquals(5, pageableCaptor.getValue().getPageSize());
    assertEquals(Sort.unsorted(), pageableCaptor.getValue().getSort());
  }
}
