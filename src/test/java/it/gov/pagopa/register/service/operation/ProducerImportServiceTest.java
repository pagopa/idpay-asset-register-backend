package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.initiative.PortalInitiativeService;
import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
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
import tools.jackson.databind.json.JsonMapper;

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
    producerImportService = new ProducerImportService(
      producersInitiativeRepository,
      JsonMapper.builder().build(),
      portalInitiativeService
    );
  }

  @Test
  void importJson_shouldMapJsonLinesAndSetInternalFields() {
    String json = """
      {"producerId":"456","initiativeId":"111","producerEmail":"producer1@test.it","producerName":"Producer 1"}
      {"producerId":"678","initiativeId":"222","producerEmail":"producer2@test.it","producerName":"Producer 2"}
      """;
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail("1234567890", "MIMIT"));
    when(portalInitiativeService.getInitiativeDetail("222")).thenReturn(initiativeDetail("1234567891", "MEF"));

    ProducerImportResultDTO result = producerImportService.importJson(json);

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
  void importJson_shouldTrimProducerInputFields() {
    String json = """
      {"producerId":" 456 ","initiativeId":" 111 ","producerEmail":" producer@test.it ","producerName":" Producer 1 "}
      """;
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());

    producerImportService.importJson(json);

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

  @ParameterizedTest
  @ValueSource(strings = {"", "\"producerEmail\":\" \",", "\"producerEmail\":\"not-an-email\","})
  void importJson_shouldSaveDefaultProducerEmailWhenMissingBlankOrInvalid(String producerEmailJsonProperty) {
    String json = """
      {"producerId":"456","initiativeId":"111",%s"producerName":"Producer 1"}
      """.formatted(producerEmailJsonProperty);
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());

    producerImportService.importJson(json);

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    ProducersInitiative savedProducer = captor.getValue().getFirst();
    assertEquals("-", savedProducer.getProducerEmail());
  }

  @Test
  void importJson_shouldMapJsonArrayAndSaveProducers() {
    String json = """
      [
        {"producerId":"456","initiativeId":"111","producerEmail":"producer@test.it","producerName":"Producer"}
      ]
      """;
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());

    ProducerImportResultDTO result = producerImportService.importJson(json);

    assertEquals("OK", result.getStatus());
    assertEquals(1, result.getTotalRecords());
    assertEquals(1, result.getImportedRecords());
    assertEquals(0, result.getFailedRecords());
  }

  @Test
  void importJson_shouldPersistInitiativeFieldsFromNestedPortalDetail() {
    String json = """
      {"producerId":"456","initiativeId":"111","producerEmail":"producer@test.it","producerName":"Producer"}
      """;
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(nestedInitiativeDetail());

    producerImportService.importJson(json);

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    ProducersInitiative savedProducer = captor.getValue().getFirst();
    assertEquals("456_111", savedProducer.getId());
    assertEquals("456", savedProducer.getProducerId());
    assertEquals("producer@test.it", savedProducer.getProducerEmail());
    assertEquals("Producer", savedProducer.getProducerName());
    assertEquals("111", savedProducer.getInitiativeId());
    assertEquals("Nested initiative", savedProducer.getInitiativeName());
    assertEquals(InitiativeStatus.APPROVED, savedProducer.getInitiativeStatus());
    assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), savedProducer.getInitiativeStartDate());
    assertEquals(LocalDateTime.of(2025, 12, 31, 0, 0), savedProducer.getInitiativeEndDate());
    assertEquals("nested-service", savedProducer.getInitiativeServiceId());
    assertEquals("Nested org", savedProducer.getInitiativeOrganizationName());
  }

  @Test
  void importJson_shouldRejectMissingInitiativeDetailField() {
    String json = """
      {"producerId":"456","initiativeId":"111","producerEmail":"producer@test.it","producerName":"Producer"}
      """;
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(InitiativeDTO.builder()
      .initiativeName("Iniziativa 1")
      .status(InitiativeStatus.PUBLISHED)
      .startDate(java.time.LocalDate.of(2025, 12, 31))
      .endDate(java.time.LocalDate.of(2026, 12, 30))
      .organizationName("MIMIT")
      .build());

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> producerImportService.importJson(json));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertTrue(exception.getReason().contains("initiativeServiceId"));
  }

  @Test
  void importJson_shouldRejectEmptyPayload() {
    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> producerImportService.importJson(" ")
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void importJson_shouldReturnRequestTimeoutWhenDbBatchFailsWithTimeout() {
    String json = """
      {"producerId":"456","initiativeId":"111","producerEmail":"producer@test.it","producerName":"Producer"}
      """;
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());

    doThrow(new QueryTimeoutException("Mongo returned 408 request timeout"))
      .when(producersInitiativeRepository).saveAll(anyList());

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> producerImportService.importJson(json)
    );

    assertEquals(HttpStatus.REQUEST_TIMEOUT, exception.getStatusCode());
    assertTrue(exception.getReason().contains("totalRecords=1"));
    assertTrue(exception.getReason().contains("importedRecords=0"));
    assertTrue(exception.getReason().contains("failedRecords=1"));
  }

  @Test
  void importJson_shouldReportImportedAndFailedRecordsWhenOnlyOneBatchFails() {
    StringBuilder json = new StringBuilder();
    for (int i = 0; i < 1001; i++) {
      json.append("""
        {"producerId":"%d","initiativeId":"111","producerEmail":"producer%d@test.it","producerName":"Producer %d"}
        """.formatted(i, i, i));
    }
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());

    when(producersInitiativeRepository.saveAll(anyList()))
      .thenAnswer(invocation -> invocation.getArgument(0))
      .thenThrow(new QueryTimeoutException("Mongo returned 408 request timeout"));

    String jsonPayload = json.toString();
    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> producerImportService.importJson(jsonPayload)
    );

    assertEquals(HttpStatus.REQUEST_TIMEOUT, exception.getStatusCode());
    assertTrue(exception.getReason().contains("totalRecords=1001"));
    assertTrue(exception.getReason().contains("importedRecords=1000"));
    assertTrue(exception.getReason().contains("failedRecords=1"));
  }

  @Test
  void importJson_shouldReturnInternalServerErrorWhenDbBatchFailsWithoutTimeout() {
    String json = """
      {"producerId":"456","initiativeId":"111","producerEmail":"producer@test.it","producerName":"Producer"}
      """;
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());

    when(producersInitiativeRepository.saveAll(anyList()))
      .thenThrow(new RuntimeException("generic db error"));

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> producerImportService.importJson(json)
    );

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    assertTrue(exception.getReason().contains("failedRecords=1"));
  }

  @Test
  void importJson_shouldCallPortalInitiativeOnceForSameProducerAndInitiative() {
    String json = """
      {"producerId":"456","initiativeId":"111","producerEmail":"producer1@test.it","producerName":"Producer 1"}
      {"producerId":"456","initiativeId":"111","producerEmail":"producer2@test.it","producerName":"Producer 2"}
      """;
    when(portalInitiativeService.getInitiativeDetail("111")).thenReturn(initiativeDetail());

    producerImportService.importJson(json);

    verify(portalInitiativeService, times(1)).getInitiativeDetail("111");
  }

  private InitiativeDTO initiativeDetail() {
    return initiativeDetail("1234567890", "MIMIT");
  }

  private InitiativeDTO initiativeDetail(String serviceId, String organizationName) {
    return InitiativeDTO.builder()
      .initiativeId("111")
      .initiativeName("Iniziativa 1")
      .status(InitiativeStatus.PUBLISHED)
      .startDate(java.time.LocalDate.of(2025, 12, 31))
      .endDate(java.time.LocalDate.of(2026, 12, 30))
      .serviceId(serviceId)
      .organizationName(organizationName)
      .build();
  }

  private InitiativeDTO nestedInitiativeDetail() {
    return InitiativeDTO.builder()
      .initiativeId("111")
      .initiativeName("Nested initiative")
      .status(InitiativeStatus.APPROVED)
      .general(new InitiativeDTO.InitiativeGeneralDTO(
        java.time.LocalDate.of(2025, 1, 1),
        java.time.LocalDate.of(2025, 12, 31)
      ))
      .additionalInfo(new InitiativeDTO.InitiativeAdditionalDTO("nested-service"))
      .organizationName("Nested org")
      .build();
  }
}
