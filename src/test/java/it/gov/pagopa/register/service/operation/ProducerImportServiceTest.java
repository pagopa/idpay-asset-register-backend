package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.dto.operation.UpdatedOperativeEmailResult;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

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
      {"producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT","operativeEmail":"test@pagopa.it"}
      {"producerId":"678","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567891","initiativeOrganizationName":"MEF"}
      """;

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
    assertEquals("111", first.getInitiativeId());
    assertEquals(InitiativeStatus.PUBLISHED, first.getInitiativeStatus());
    assertEquals(LocalDateTime.of(2025, 12, 31, 22, 0), first.getInitiativeStartDate());
    assertEquals(LocalDateTime.of(2026, 12, 30, 22, 0), first.getInitiativeEndDate());
    assertEquals("test@pagopa.it", first.getOperativeEmail());
    assertEquals("CSV", first.getSource());
    assertTrue(first.getEnabled());
    assertNotNull(first.getCreatedAt());
  }

  @Test
  void importJson_withInvalidEmailFormat_shouldSaveWithoutEmail() {
    String json = """
      {"producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT","operativeEmail":"invalid-email-format"}
      """;

    ProducerImportResultDTO result = producerImportService.importJson(json);

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    ProducersInitiative saved = captor.getValue().getFirst();
    assertNull(saved.getOperativeEmail());
    assertEquals("OK", result.getStatus());
  }

  @Test
  void importJson_withLocalDateFormat_shouldParseCorrectly() {
    String json = """
      {"producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00","initiativeEndDate":"2026-12-30T22:00:00","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT"}
      """;

    ProducerImportResultDTO result = producerImportService.importJson(json);

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    ProducersInitiative saved = captor.getValue().getFirst();
    assertEquals(LocalDateTime.of(2025, 12, 31, 22, 0), saved.getInitiativeStartDate());
    assertEquals("OK", result.getStatus());
  }

  @Test
  void importJson_withInvalidDateFormat_shouldThrowBadRequest() {
    String json = """
      {"producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"invalid-date","initiativeEndDate":"2026-12-30T22:00:00","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT"}
      """;

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> producerImportService.importJson(json));
    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void importJson_shouldRejectInvalidStatus() {
    String json = """
      {"producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"ACTIVE","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT"}
      """;

    assertThrows(ResponseStatusException.class, () -> producerImportService.importJson(json));
  }

  @Test
  void importJson_shouldMapJsonArrayAndSaveProducers() {
    String json = """
      [
        {"producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","InitiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT"}
      ]
      """;

    ProducerImportResultDTO result = producerImportService.importJson(json);

    assertEquals("OK", result.getStatus());
    assertEquals(1, result.getTotalRecords());
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
      {"producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT"}
      """;

    doThrow(new QueryTimeoutException("Mongo returned 408 request timeout"))
      .when(producersInitiativeRepository).saveAll(anyList());

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> producerImportService.importJson(json)
    );

    assertEquals(HttpStatus.REQUEST_TIMEOUT, exception.getStatusCode());
  }

  @Test
  void importJson_shouldReportImportedAndFailedRecordsWhenOnlyOneBatchFails() {
    StringBuilder json = new StringBuilder();
    for (int i = 0; i < 1001; i++) {
      json.append("""
        {"producerId":"%d","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT"}
        """.formatted(i));
    }

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
      {"producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT"}
      """;

    when(producersInitiativeRepository.saveAll(anyList()))
      .thenThrow(new RuntimeException("generic db error"));

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> producerImportService.importJson(json)
    );

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
  }

  @Test
  void updateOperativeEmail_Success() {
    String orgId = "org123";
    String initiativeId = "init123";
    String key = orgId + "_" + initiativeId;
    String newEmail = "new@pagopa.it";

    ProducersInitiative initiative = new ProducersInitiative();
    initiative.setId(key);

    when(producersInitiativeRepository.findById(key)).thenReturn(Optional.of(initiative));

    UpdatedOperativeEmailResult result = producerImportService.updateOperativeEmail(orgId, initiativeId, newEmail);

    assertEquals("OK", result.getStatus());
    assertNull(result.getErrorKey());
    assertEquals(newEmail, initiative.getOperativeEmail());
    assertNotNull(initiative.getUpdatedAt());
    verify(producersInitiativeRepository).save(initiative);
  }

  @Test
  void updateOperativeEmail_withInvalidEmailFormat_shouldReturnKo() {
    String orgId = "org123";
    String initiativeId = "init123";

    UpdatedOperativeEmailResult result = producerImportService.updateOperativeEmail(orgId, initiativeId, "invalid-email");

    assertEquals("KO", result.getStatus());
    assertEquals(AssetRegisterConstants.UploadEmailKeyConstant.EMAIL_WRONG_ERROR_KEY, result.getErrorKey());
    verify(producersInitiativeRepository, never()).findById(anyString());
    verify(producersInitiativeRepository, never()).save(any());
  }

  @Test
  void updateOperativeEmail_initiativeNotFound_shouldReturnKo() {
    String orgId = "org123";
    String initiativeId = "init123";
    String key = orgId + "_" + initiativeId;

    when(producersInitiativeRepository.findById(key)).thenReturn(Optional.empty());

    UpdatedOperativeEmailResult result = producerImportService.updateOperativeEmail(orgId, initiativeId, "valid@email.com");

    assertEquals("KO", result.getStatus());
    assertEquals(AssetRegisterConstants.UploadEmailKeyConstant.EMAIL_INITATIVE_ERROR_KEY, result.getErrorKey());
    verify(producersInitiativeRepository, never()).save(any());
  }

  @Test
  void importJson_withNullEmail_shouldSaveSuccessfullyWithoutEmail() {
    String json = """
      {"producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT"}
      """;

    ProducerImportResultDTO result = producerImportService.importJson(json);

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    ProducersInitiative saved = captor.getValue().getFirst();
    assertNull(saved.getOperativeEmail());
    assertEquals("OK", result.getStatus());
  }

  @Test
  void importJson_withBlankEmail_shouldSaveSuccessfullyWithoutEmail() {
    String json = """
      {"producerId":"456","initiativeId":"111","initiativeName":"Iniziativa 1","initiativeStatus":"PUBLISHED","initiativeStartDate":"2025-12-31T22:00:00.000Z","initiativeEndDate":"2026-12-30T22:00:00.000Z","initiativeServiceId":"1234567890","initiativeOrganizationName":"MIMIT","operativeEmail":"   "}
      """;

    ProducerImportResultDTO result = producerImportService.importJson(json);

    ArgumentCaptor<List<ProducersInitiative>> captor = ArgumentCaptor.forClass(List.class);
    verify(producersInitiativeRepository).saveAll(captor.capture());

    ProducersInitiative saved = captor.getValue().getFirst();
    assertNull(saved.getOperativeEmail());
    assertEquals("OK", result.getStatus());
  }
}
