package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.ProducerImportJsonDTO;
import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  private final ProducersInitiativeRepository producersInitiativeRepository;
  private final ObjectMapper objectMapper;


  public ProducerImportResultDTO importJson(String json) {
    log.info("[IMPORT_PRODUCERS] - Importing producers from json payload");

    try {
      List<ProducersInitiative> producers = parseJson(json);
      producersInitiativeRepository.saveAll(producers);

      log.info("[IMPORT_PRODUCERS] - Imported {} producer records from json", producers.size());
      return ProducerImportResultDTO.builder()
        .status("OK")
        .importedRecords(producers.size())
        .build();
    } catch (JacksonException e) {
      log.warn("[IMPORT_PRODUCERS] - Invalid json payload: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON payload", e);
    } catch (IllegalArgumentException e) {
      log.warn("[IMPORT_PRODUCERS] - Invalid json content: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
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
