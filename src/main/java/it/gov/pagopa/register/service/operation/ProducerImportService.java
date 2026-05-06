package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.ProducerImportJsonDTO;
import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProducerImportService {

  private static final String CSV_SOURCE = "CSV";
  private static final int SAVE_BATCH_SIZE = 1000;

  private final ProducersInitiativeRepository producersInitiativeRepository;
  private final ObjectMapper objectMapper;


  public ProducerImportResultDTO importJson(String json) {
    log.info("[IMPORT_PRODUCERS] - Importing producers from json payload");

    try {
      List<ProducersInitiative> producers = parseJson(json);
      ProducerImportResultDTO result = saveInBatches(producers);

      log.info("[IMPORT_PRODUCERS] - Import completed. totalRecords={}, importedRecords={}, failedRecords={}",
        result.getTotalRecords(), result.getImportedRecords(), result.getFailedRecords());
      return result;
    } catch (JacksonException e) {
      log.warn("[IMPORT_PRODUCERS] - Invalid json payload: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON payload", e);
    } catch (IllegalArgumentException e) {
      log.warn("[IMPORT_PRODUCERS] - Invalid json content: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }

  private ProducerImportResultDTO saveInBatches(List<ProducersInitiative> producers) {
    int importedRecords = 0;
    int failedRecords = 0;
    RuntimeException lastError = null;

    log.info("[IMPORT_PRODUCERS] - Saving {} producer records in batches of {}",
      producers.size(), SAVE_BATCH_SIZE);

    for (int start = 0; start < producers.size(); start += SAVE_BATCH_SIZE) {
      int end = Math.min(start + SAVE_BATCH_SIZE, producers.size());
      List<ProducersInitiative> batch = producers.subList(start, end);

      try {
        producersInitiativeRepository.saveAll(batch);
        importedRecords += batch.size();
        log.info("[IMPORT_PRODUCERS] - Saved batch {}/{} records. importedRecords={}, remainingRecords={}",
          start + 1, end, importedRecords, producers.size() - importedRecords - failedRecords);
      } catch (RuntimeException e) {
        failedRecords += batch.size();
        lastError = e;
        log.error("[IMPORT_PRODUCERS] - Failed saving batch {}/{} records. importedRecords={}, failedRecords={}, error={}",
          start + 1, end, importedRecords, failedRecords, e.getMessage(), e);
      }
    }

    if (failedRecords > 0) {
      String message = "Producer import partially failed. totalRecords=%d, importedRecords=%d, failedRecords=%d"
        .formatted(producers.size(), importedRecords, failedRecords);
      HttpStatus status = isRequestTimeout(lastError) ? HttpStatus.REQUEST_TIMEOUT : HttpStatus.INTERNAL_SERVER_ERROR;
      log.error("[IMPORT_PRODUCERS] - {}", message, lastError);
      throw new ResponseStatusException(status, message, lastError);
    }

    return ProducerImportResultDTO.builder()
      .status("OK")
      .totalRecords(producers.size())
      .importedRecords(importedRecords)
      .failedRecords(0)
      .message("Producer import completed successfully")
      .build();
  }

  private boolean isRequestTimeout(Throwable error) {
    Throwable current = error;
    while (current != null) {
      if (current instanceof QueryTimeoutException || current instanceof TransientDataAccessResourceException) {
        return true;
      }
      String message = current.getMessage();
      if (message != null) {
        String lowerCaseMessage = message.toLowerCase();
        if (message.contains("408")
          || lowerCaseMessage.contains("request timeout")
          || lowerCaseMessage.contains("timed out")
          || lowerCaseMessage.contains("timeout")) {
          return true;
        }
      }
      current = current.getCause();
    }
    return false;
  }

  private List<ProducersInitiative> parseJson(String json) throws JacksonException {
    if (json == null || json.isBlank()) {
      throw new IllegalArgumentException("JSON payload is empty");
    }

    String trimmedJson = json.strip();

    List<ProducerImportJsonDTO> records;

    if (trimmedJson.startsWith("[")) {
      List<Map<String, Object>> jsonRecords = objectMapper.readValue(
        trimmedJson, new TypeReference<List<Map<String, Object>>>() {
        });
      records = jsonRecords.stream()
        .map(this::toJsonDTO)
        .toList();
    } else {
      records = Arrays.stream(trimmedJson.split("\\R"))
        .map(String::strip)
        .filter(line -> !line.isBlank())
        .map(this::readJsonLine)
        .toList();
    }

    if (records.isEmpty()) {
      throw new IllegalArgumentException("JSON payload does not contain records");
    }

    LocalDateTime now = LocalDateTime.now();
    return records.stream()
      .map(record -> toProducer(record, now))
      .toList();
  }

  private ProducerImportJsonDTO readJsonLine(String line) {
    try {
      Map<String, Object> record = objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {
      });
      return toJsonDTO(record);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Invalid JSON line", e);
    }
  }

  private ProducerImportJsonDTO toJsonDTO(Map<String, Object> record) {
    ProducerImportJsonDTO dto = new ProducerImportJsonDTO();
    dto.setProducerId(stringValue(record.get("producerId")));
    dto.setInitiativeId(stringValue(record.get("initiativeId")));
    dto.setInitiativeName(stringValue(record.get("initiativeName")));
    dto.setInitiativeStatus(stringValue(record.get("initiativeStatus"), record.get("InitiativeStatus")));
    dto.setInitiativeStartDate(stringValue(record.get("initiativeStartDate")));
    dto.setInitiativeEndDate(stringValue(record.get("initiativeEndDate")));
    dto.setInitiativeServiceId(stringValue(record.get("initiativeServiceId")));
    dto.setInitiativeOrganizationName(stringValue(record.get("initiativeOrganizationName")));
    return dto;
  }

  private String stringValue(Object... values) {
    return Arrays.stream(values)
      .filter(value -> value != null)
      .findFirst()
      .map(Object::toString)
      .orElse(null);
  }

  private ProducersInitiative toProducer(ProducerImportJsonDTO dto, LocalDateTime now) {
    String producerId = requiredValue(dto.getProducerId(), "producerId");
    String initiativeId = requiredValue(dto.getInitiativeId(), "initiativeId");

    return ProducersInitiative.builder()
      .id(producerId + "_" + initiativeId)
      .producerId(producerId)
      .initiativeId(initiativeId)
      .initiativeName(requiredValue(dto.getInitiativeName(), "initiativeName"))
      .initiativeStatus(parseInitiativeStatus(requiredValue(dto.getInitiativeStatus(), "initiativeStatus")))
      .initiativeStartDate(parseJsonDate(
        requiredValue(dto.getInitiativeStartDate(), "initiativeStartDate"), "initiativeStartDate"))
      .initiativeEndDate(parseJsonDate(
        requiredValue(dto.getInitiativeEndDate(), "initiativeEndDate"), "initiativeEndDate"))
      .initiativeServiceId(requiredValue(dto.getInitiativeServiceId(), "initiativeServiceId"))
      .initiativeOrganizationName(requiredValue(dto.getInitiativeOrganizationName(), "initiativeOrganizationName"))
      .source(CSV_SOURCE)
      .enabled(Boolean.TRUE)
      .createdAt(now)
      .updatedAt(now)
      .build();
  }

  private String requiredValue(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing required field [%s]".formatted(fieldName));
    }
    return value;
  }

  private InitiativeStatus parseInitiativeStatus(String value) {
    try {
      return InitiativeStatus.valueOf(value);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid initiativeStatus [%s]".formatted(value), e);
    }
  }

  private LocalDateTime parseJsonDate(String value, String fieldName) {
    try {
      return OffsetDateTime.parse(value).toLocalDateTime();
    } catch (DateTimeParseException e) {
      try {
        return LocalDateTime.parse(value);
      } catch (DateTimeParseException ex) {
        throw new IllegalArgumentException("Invalid date field [%s]".formatted(fieldName), ex);
      }
    }
  }
}
