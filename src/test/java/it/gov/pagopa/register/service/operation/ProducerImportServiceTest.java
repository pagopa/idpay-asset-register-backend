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
import org.springframework.mock.web.MockMultipartFile;
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
  void importCsv_shouldMapCsvRowsAndSaveProducers() {
    MockMultipartFile file = new MockMultipartFile(
      "csv",
      "produttori.csv",
      "text/csv",
      """
        producerId,initiativeId,initiativeName,initiativeStatus,initiativeStartDate,initiativeEndDate,initiativeServiceId,initiativeOrganizationName
        456,111,Iniziativa 1,PUBLISHED,01/01/26,31/12/26,1234567890,MIMIT
        678,111,Iniziativa 1,PUBLISHED,01/01/26,31/12/26,1234567891,MEF
        """.getBytes()
    );

    ProducerImportResultDTO result = producerImportService.importCsv(file);

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
    assertEquals("Iniziativa 1", first.getInitiativeName());
    assertEquals(InitiativeStatus.PUBLISHED, first.getInitiativeStatus());
    assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), first.getInitiativeStartDate());
    assertEquals(LocalDateTime.of(2026, 12, 31, 0, 0), first.getInitiativeEndDate());
    assertEquals("1234567890", first.getInitiativeServiceId());
    assertEquals("MIMIT", first.getInitiativeOrganizationName());
    assertEquals("CSV", first.getSource());
    assertTrue(first.getEnabled());
    assertNotNull(first.getCreatedAt());
    assertEquals(first.getCreatedAt(), first.getUpdatedAt());
  }

  @Test
  void importCsv_shouldRejectInvalidDate() {
    MockMultipartFile file = new MockMultipartFile(
      "csv",
      "produttori.csv",
      "text/csv",
      """
        producerId,initiativeId,initiativeName,initiativeStatus,initiativeStartDate,initiativeEndDate,initiativeServiceId,initiativeOrganizationName
        456,111,Iniziativa 1,PUBLISHED,wrong-date,31/12/26,1234567890,MIMIT
        """.getBytes()
    );

    assertThrows(ResponseStatusException.class, () -> producerImportService.importCsv(file));
  }

  @Test
  void importJson_shouldMapJsonLinesAndSaveProducers() {
    String json = """
      {"_id":"456_111","producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","InitiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT","enabled":true,"source":"CSV","createdAt":"2026-05-04T15:19:45.879Z","updateAt":"2026-05-04T15:19:45.879Z"}
      {"_id":"678_111","producerId":"678","initiativeId":"111","initiativeName":"Iniziativa 1","InitiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567891","initiativeOrganizationName":"MEF","enabled":true,"source":"CSV","createdAt":"2026-05-04T15:19:45.879Z","updateAt":"2026-05-04T15:19:45.879Z"}
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
    assertEquals(LocalDateTime.of(2026, 5, 4, 15, 19, 45, 879000000), first.getCreatedAt());
    assertEquals(LocalDateTime.of(2026, 5, 4, 15, 19, 45, 879000000), first.getUpdatedAt());
  }

  @Test
  void importJson_shouldRejectInvalidId() {
    String json = """
      {"_id":"wrong","producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","InitiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT","enabled":true,"source":"CSV","createdAt":"2026-05-04T15:19:45.879Z","updateAt":"2026-05-04T15:19:45.879Z"}
      """;

    assertThrows(ResponseStatusException.class, () -> producerImportService.importJson(json));
  }
}
