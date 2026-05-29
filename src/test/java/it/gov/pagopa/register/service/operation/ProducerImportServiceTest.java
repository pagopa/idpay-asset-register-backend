package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.initiative.PortalInitiativeService;
import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.dto.operation.ProducerInitiativeRequestDTO;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ProducerImportServiceTest {

  @Mock
  private ProducersInitiativeRepository producersInitiativeRepository;

  @Mock
  private PortalInitiativeService portalInitiativeService;

  private ProducerImportService producerImportService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    producerImportService = new ProducerImportService(producersInitiativeRepository, portalInitiativeService);
  }

  @Test
  void importProducers_shouldMapRequestsAndEnrichInitiativeFields() {
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail("1234567890", "MIMIT"));
    when(portalInitiativeService.getInitiativeDetail("222")).thenReturn(initiativeDetail("1234567891", "MEF"));

    ProducerImportResultDTO result = producerImportService.importProducers(List.of(
      producerRequest("456", "111", "Producer 1", "producer1@test.it"),
      producerRequest("678", "222", "Producer 2", "producer2@test.it")
    ));

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    List<ProducersInitiative> savedProducers = captor.getValue();
    assertEquals("OK", result.getStatus());
    assertEquals(2, result.getTotalRecords());
    assertEquals(2, result.getImportedRecords());
    assertEquals(0, result.getFailedRecords());
    assertEquals("Producer import completed successfully", result.getMessage());
    assertEquals(2, savedProducers.size());

    ProducersInitiative first = savedProducers.getFirst();
    assertEquals("456_111", first.getId());
    assertEquals("456", first.getProducerId());
    assertEquals("producer1@test.it", first.getProducerEmail());
    assertEquals("Producer 1", first.getProducerName());
    assertEquals("111", first.getInitiativeId());
    assertEquals("Iniziativa 1", first.getInitiativeName());
    assertEquals(InitiativeStatus.PUBLISHED, first.getInitiativeStatus());
    assertEquals(LocalDateTime.of(2025, 12, 31, 0, 0), first.getInitiativeStartDate());
    assertEquals(LocalDateTime.of(2026, 12, 30, 0, 0), first.getInitiativeEndDate());
    assertEquals("1234567890", first.getInitiativeServiceId());
    assertEquals("MIMIT", first.getInitiativeOrganizationName());
    assertEquals("CSV", first.getSource());
    assertTrue(first.getEnabled());
    assertNotNull(first.getCreatedAt());
    assertEquals(first.getCreatedAt(), first.getUpdatedAt());
  }

  @Test
  void importProducers_shouldTrimProducerInputFields() {
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());

    producerImportService.importProducers(List.of(
      producerRequest(" 456 ", " 111 ", " Producer 1 ", " producer@test.it ")
    ));

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    ProducersInitiative savedProducer = captor.getValue().getFirst();
    assertEquals("456_111", savedProducer.getId());
    assertEquals("456", savedProducer.getProducerId());
    assertEquals("111", savedProducer.getInitiativeId());
    assertEquals("producer@test.it", savedProducer.getProducerEmail());
    assertEquals("Producer 1", savedProducer.getProducerName());
    verify(portalInitiativeService).getInitiativeDetail("111");
  }

  @Test
  void importProducers_shouldPersistInitiativeFieldsFromNestedPortalDetail() {
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(nestedInitiativeDetail());

    producerImportService.importProducers(List.of(
      producerRequest("456", "111", "Producer 1", "producer@test.it")
    ));

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    ProducersInitiative savedProducer = captor.getValue().getFirst();
    assertEquals("456_111", savedProducer.getId());
    assertEquals("Nested initiative", savedProducer.getInitiativeName());
    assertEquals(InitiativeStatus.APPROVED, savedProducer.getInitiativeStatus());
    assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), savedProducer.getInitiativeStartDate());
    assertEquals(LocalDateTime.of(2025, 12, 31, 0, 0), savedProducer.getInitiativeEndDate());
    assertEquals("nested-service", savedProducer.getInitiativeServiceId());
    assertEquals("Nested org", savedProducer.getInitiativeOrganizationName());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "not-an-email"})
  void importProducers_shouldSaveNullProducerEmailWhenMissingBlankOrInvalid(String producerEmail) {
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());

    producerImportService.importProducers(List.of(
      producerRequest("456", "111", "Producer 1", producerEmail)
    ));

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    ProducersInitiative savedProducer = captor.getValue().getFirst();
    assertNull(savedProducer.getProducerEmail());
  }

  @Test
  void importProducers_shouldSaveNullProducerEmailWhenMissing() {
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());

    producerImportService.importProducers(List.of(
      producerRequest("456", "111", "Producer 1", null)
    ));

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    ProducersInitiative savedProducer = captor.getValue().getFirst();
    assertNull(savedProducer.getProducerEmail());
  }

  @Test
  void importProducers_shouldReportFailedRecordWhenProducerRequiredFieldIsMissing() {
    ProducerImportResultDTO result = producerImportService.importProducers(
      List.of(producerRequest("456", " ", "Producer 1", null))
    );

    assertEquals("PARTIAL", result.getStatus());
    assertEquals(1, result.getTotalRecords());
    assertEquals(0, result.getImportedRecords());
    assertEquals(1, result.getFailedRecords());
    verify(producersInitiativeRepository, never()).saveAll(anyList());
    verifyNoInteractions(portalInitiativeService);
  }

  @Test
  void importProducers_shouldSkipRecordWhenProducerNameIsMissingAndSaveOthers() {
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());

    ProducerImportResultDTO result = producerImportService.importProducers(List.of(
      producerRequest("456", "111", "Producer 1", "producer1@test.it"),
      producerRequest("999", "111", " ", "producer2@test.it")
    ));

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    assertEquals("PARTIAL", result.getStatus());
    assertEquals(2, result.getTotalRecords());
    assertEquals(1, result.getImportedRecords());
    assertEquals(1, result.getFailedRecords());
    assertEquals(1, captor.getValue().size());
    assertEquals("456_111", captor.getValue().getFirst().getId());
  }

  @Test
  void importProducers_shouldRejectEmptyPayload() {
    List<ProducerInitiativeRequestDTO> requests = List.of();

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> producerImportService.importProducers(requests)
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void importProducers_shouldReportFailedRecordWhenInitiativeDetailFieldIsMissing() {
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(InitiativeDTO.builder()
      .initiativeName("Iniziativa 1")
      .status(InitiativeStatus.PUBLISHED)
      .general(new InitiativeDTO.InitiativeGeneralDTO(
        LocalDate.of(2025, 12, 31),
        LocalDate.of(2026, 12, 30)
      ))
      .additionalInfo(new InitiativeDTO.InitiativeAdditionalDTO(null))
      .organizationName("MIMIT")
      .build());

    ProducerImportResultDTO result = producerImportService.importProducers(
      List.of(producerRequest("456", "111", "Producer 1", null))
    );

    assertEquals("PARTIAL", result.getStatus());
    assertEquals(1, result.getTotalRecords());
    assertEquals(0, result.getImportedRecords());
    assertEquals(1, result.getFailedRecords());
    verify(producersInitiativeRepository, never()).saveAll(anyList());
  }

  @Test
  void importProducers_shouldSkipRecordWhenInitiativeDetailFailsAndSaveOthers() {
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());
    when(portalInitiativeService.getInitiativeDetail("wrong")).thenThrow(new RuntimeException("initiative not found"));

    ProducerImportResultDTO result = producerImportService.importProducers(List.of(
      producerRequest("456", "111", "Producer 1", "producer1@test.it"),
      producerRequest("999", "wrong", "Producer wrong", "wrong@test.it")
    ));

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    assertEquals("PARTIAL", result.getStatus());
    assertEquals(2, result.getTotalRecords());
    assertEquals(1, result.getImportedRecords());
    assertEquals(1, result.getFailedRecords());
    assertEquals(1, captor.getValue().size());
    assertEquals("456_111", captor.getValue().getFirst().getId());
  }

  @Test
  void importProducers_shouldCallPortalInitiativeOnceForSameInitiative() {
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());

    producerImportService.importProducers(List.of(
      producerRequest("456", "111", "Producer 1", "producer1@test.it"),
      producerRequest("678", "111", "Producer 2", "producer2@test.it")
    ));

    verify(portalInitiativeService, times(1)).getInitiativeDetail("111");
  }

  @Test
  void importProducers_shouldReturnRequestTimeoutWhenDbBatchFailsWithTimeout() {
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());
    doThrow(new QueryTimeoutException("Mongo returned 408 request timeout"))
      .when(producersInitiativeRepository).saveAll(anyList());
    List<ProducerInitiativeRequestDTO> requests = List.of(producerRequest("456", "111", "Producer", null));

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> producerImportService.importProducers(requests)
    );

    assertEquals(HttpStatus.REQUEST_TIMEOUT, exception.getStatusCode());
    assertTrue(exception.getReason().contains("totalRecords=1"));
    assertTrue(exception.getReason().contains("importedRecords=0"));
    assertTrue(exception.getReason().contains("failedRecords=1"));
  }

  @Test
  void importProducers_shouldReportImportedAndFailedRecordsWhenOnlyOneBatchFails() {
    List<ProducerInitiativeRequestDTO> requests = java.util.stream.IntStream.range(0, 1001)
      .mapToObj(i -> producerRequest(String.valueOf(i), "111", "Producer %d".formatted(i), "producer%d@test.it".formatted(i)))
      .toList();
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());
    when(producersInitiativeRepository.saveAll(anyList()))
      .thenAnswer(invocation -> invocation.getArgument(0))
      .thenThrow(new QueryTimeoutException("Mongo returned 408 request timeout"));

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> producerImportService.importProducers(requests)
    );

    assertEquals(HttpStatus.REQUEST_TIMEOUT, exception.getStatusCode());
    assertTrue(exception.getReason().contains("totalRecords=1001"));
    assertTrue(exception.getReason().contains("importedRecords=1000"));
    assertTrue(exception.getReason().contains("failedRecords=1"));
  }

  @Test
  void importProducers_shouldReturnInternalServerErrorWhenDbBatchFailsWithoutTimeout() {
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());
    when(producersInitiativeRepository.saveAll(anyList()))
      .thenThrow(new RuntimeException("generic db error"));
    List<ProducerInitiativeRequestDTO> requests = List.of(producerRequest("456", "111", "Producer", null));

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> producerImportService.importProducers(requests)
    );

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    assertTrue(exception.getReason().contains("failedRecords=1"));
  }

  private ProducerInitiativeRequestDTO producerRequest(String producerId, String initiativeId, String producerName, String producerEmail) {
    ProducerInitiativeRequestDTO request = new ProducerInitiativeRequestDTO();
    request.setProducerId(producerId);
    request.setInitiativeId(initiativeId);
    request.setProducerName(producerName);
    request.setProducerEmail(producerEmail);
    return request;
  }

  private InitiativeDTO initiativeDetail() {
    return initiativeDetail("1234567890", "MIMIT");
  }

  private InitiativeDTO initiativeDetail(String serviceId, String organizationName) {
    return InitiativeDTO.builder()
      .initiativeId("111")
      .initiativeName("Iniziativa 1")
      .status(InitiativeStatus.PUBLISHED)
      .general(new InitiativeDTO.InitiativeGeneralDTO(
        LocalDate.of(2025, 12, 31),
        LocalDate.of(2026, 12, 30)
      ))
      .additionalInfo(new InitiativeDTO.InitiativeAdditionalDTO(serviceId))
      .organizationName(organizationName)
      .build();
  }

  private InitiativeDTO nestedInitiativeDetail() {
    return InitiativeDTO.builder()
      .initiativeId("111")
      .initiativeName("Nested initiative")
      .status(InitiativeStatus.APPROVED)
      .general(new InitiativeDTO.InitiativeGeneralDTO(
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 12, 31)
      ))
      .additionalInfo(new InitiativeDTO.InitiativeAdditionalDTO("nested-service"))
      .organizationName("Nested org")
      .build();
  }
}
