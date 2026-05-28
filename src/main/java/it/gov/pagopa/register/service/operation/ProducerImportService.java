package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.initiative.PortalInitiativeService;
import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.ProducerImportJsonDTO;
import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static it.gov.pagopa.register.constants.ValidationPatterns.EMAIL_PATTERN;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProducerImportService {

  private static final String CSV_SOURCE = "CSV";
  private static final int SAVE_BATCH_SIZE = 1000;
  private static final Pattern EMAIL_VALIDATION_PATTERN = Pattern.compile(EMAIL_PATTERN);
  private static final String MISSING_REQUIRED_FIELD_MESSAGE = "Missing required field [%s]";
  private static final String INITIATIVE_ID = "initiativeId";

  private final ProducersInitiativeRepository producersInitiativeRepository;
  private final ObjectMapper objectMapper;
  private final PortalInitiativeService portalInitiativeService;


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
    if (StringUtils.isBlank(json)) {
      throw new IllegalArgumentException("JSON payload is empty");
    }

    String trimmedJson = StringUtils.strip(json);

    List<ProducerImportJsonDTO> records;

    if (trimmedJson.startsWith("[")) {
      records = objectMapper.readValue(
        trimmedJson, new TypeReference<List<ProducerImportJsonDTO>>() {
        });
    } else {
      records = Arrays.stream(trimmedJson.split("\\R"))
        .map(StringUtils::strip)
        .filter(StringUtils::isNotBlank)
        .map(this::readJsonLine)
        .toList();
    }

    if (records.isEmpty()) {
      throw new IllegalArgumentException("JSON payload does not contain records");
    }

    LocalDateTime now = LocalDateTime.now();
    Map<String, InitiativeDTO> initiativeDetails = new HashMap<>();
    return records.stream()
      .map(producerImportJsonDTO -> {
        validateProducerInput(producerImportJsonDTO);
        return toProducer(
          producerImportJsonDTO,
          getInitiativeDetail(producerImportJsonDTO, initiativeDetails),
          now);
      })
      .toList();
  }

  private void validateProducerInput(ProducerImportJsonDTO dto) {
    requiredValue(dto.getProducerId(), "producerId");
    requiredValue(dto.getInitiativeId(), INITIATIVE_ID);
    requiredValue(dto.getProducerName(), "producerName");
  }

  private InitiativeDTO getInitiativeDetail(ProducerImportJsonDTO dto, Map<String, InitiativeDTO> initiativeDetails) {
    String initiativeId = requiredValue(dto.getInitiativeId(), INITIATIVE_ID);
    return initiativeDetails.computeIfAbsent(initiativeId, portalInitiativeService::getInitiativeDetail);
  }

  private ProducerImportJsonDTO readJsonLine(String line) {
    try {
      return objectMapper.readValue(line, ProducerImportJsonDTO.class);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Invalid JSON line", e);
    }
  }

  private ProducersInitiative toProducer(ProducerImportJsonDTO dto, InitiativeDTO initiativeDetail, LocalDateTime now) {
    String producerId = requiredValue(dto.getProducerId(), "producerId");
    String initiativeId = requiredValue(dto.getInitiativeId(), INITIATIVE_ID);
    String producerEmail = optionalEmail(dto.getProducerEmail());
    String producerName = requiredValue(dto.getProducerName(), "producerName");
    if (initiativeDetail == null) {
      throw new IllegalArgumentException("Initiative detail not found for producerId [%s] and initiativeId [%s]"
        .formatted(producerId, initiativeId));
    }

    return ProducersInitiative.builder()
      .id(producerId + "_" + initiativeId)
      .producerId(producerId)
      .producerEmail(producerEmail)
      .producerName(producerName)
      .initiativeId(initiativeId)
      .initiativeName(requiredValue(initiativeDetail.getInitiativeName(), "initiativeName"))
      .initiativeStatus(requiredStatus(initiativeDetail.getStatus(), "initiativeStatus"))
      .initiativeStartDate(requiredDate(initiativeDetail.getStartDate(), "initiativeStartDate").atStartOfDay())
      .initiativeEndDate(requiredDate(initiativeDetail.getEndDate(), "initiativeEndDate").atStartOfDay())
      .initiativeServiceId(requiredValue(initiativeDetail.getServiceId(), "initiativeServiceId"))
      .initiativeOrganizationName(requiredValue(initiativeDetail.getOrganizationName(), "initiativeOrganizationName"))
      .source(CSV_SOURCE)
      .enabled(Boolean.TRUE)
      .createdAt(now)
      .updatedAt(now)
      .build();
  }

  private String requiredValue(String value, String fieldName) {
    String cleanedValue = StringUtils.strip(value);
    if (StringUtils.isBlank(cleanedValue)) {
      throw new IllegalArgumentException(MISSING_REQUIRED_FIELD_MESSAGE.formatted(fieldName));
    }
    return cleanedValue;
  }

  private String optionalEmail(String value) {
    String email = StringUtils.strip(value);
    if (StringUtils.isBlank(email)) {
      return null;
    }
    return EMAIL_VALIDATION_PATTERN.matcher(email).matches()
      ? email
      : null;
  }

  private LocalDate requiredDate(LocalDate value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(MISSING_REQUIRED_FIELD_MESSAGE.formatted(fieldName));
    }
    return value;
  }

  private InitiativeStatus requiredStatus(InitiativeStatus value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(MISSING_REQUIRED_FIELD_MESSAGE.formatted(fieldName));
    }
    return value;
  }
}
