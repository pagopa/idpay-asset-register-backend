package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

class ProducerImportServiceTest {

  @Mock
  private ProducersInitiativeRepository producersInitiativeRepository;

  private ProducerImportService producerImportService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    producerImportService = new ProducerImportService(producersInitiativeRepository, JsonMapper.builder().build());
  }

  @Test
  void importJson_shouldMapJsonLinesAndSetInternalFields() {
    String json = """
      {"producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT"}
      {"producerId":"678","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567891","initiativeOrganizationName":"MEF"}
      """;

    ProducerImportResultDTO result = producerImportService.importJson(json);

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    List<ProducersInitiative> savedProducers = captor.getValue();
    assertEquals("OK", result.getStatus());
    assertEquals(2, result.getImportedRecords());
    assertEquals(2, savedProducers.size());

    ProducersInitiative first = savedProducers.getFirst();
    assertEquals("456_111", first.getId());
    assertEquals("456", first.getProducerId());
    assertEquals("111", first.getInitiativeId());
    assertEquals(InitiativeStatus.PUBLISHED, first.getInitiativeStatus());
    assertEquals(LocalDateTime.of(2025, 12, 31, 22, 0), first.getInitiativeStartDate());
    assertEquals(LocalDateTime.of(2026, 12, 30, 22, 0), first.getInitiativeEndDate());
    assertEquals("CSV", first.getSource());
    assertTrue(first.getEnabled());
    assertNotNull(first.getCreatedAt());
    assertEquals(first.getCreatedAt(), first.getUpdatedAt());
  }

  @Test
  void importJson_shouldRejectInvalidStatus() {
    String json = """
      {"producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"ACTIVE","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT"}
      """;

    assertThrows(ResponseStatusException.class, () -> producerImportService.importJson(json));
  }
}
