package it.gov.pagopa.register.service.operation;

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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    List<ProducerImportJsonDTO> records = trimmedJson.startsWith("[")
      ? objectMapper.readValue(
        trimmedJson, new TypeReference<List<ProducerImportJsonDTO>>() {
        })
      : Arrays.stream(trimmedJson.split("\\R"))
        .map(StringUtils::strip)
        .filter(StringUtils::isNotBlank)
        .map(this::readJsonLine)
        .toList();

    if (records.isEmpty()) {
      throw new IllegalArgumentException("JSON payload does not contain records");
    }

    LocalDateTime now = LocalDateTime.now();
    return records.stream()
      .map(this::toProducerInput)
      .map(producerInput -> toProducer(
        producerInput,
        initiativeDetails.computeIfAbsent(producerInput.initiativeId(), portalInitiativeService::getInitiativeDetail),
        now))
      .toList();
  }

  private ProducerInput toProducerInput(ProducerImportJsonDTO dto) {
    return new ProducerInput(
      requiredValue(dto.getProducerId(), "producerId"),
      requiredValue(dto.getInitiativeId(), INITIATIVE_ID),
      optionalEmail(dto.getProducerEmail()),
      requiredValue(dto.getProducerName(), "producerName")
    );
  }

  private ProducerImportJsonDTO readJsonLine(String line) {
    try {
      Map<String, Object> producerFields = objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {
      });
      return toJsonDTO(producerFields);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Invalid JSON line", e);
    }
  }

  private ProducersInitiative toProducer(ProducerInput producerInput, InitiativeDTO initiativeDetail, LocalDateTime now) {
    if (initiativeDetail == null) {
      throw new IllegalArgumentException("Initiative detail not found for producerId [%s] and initiativeId [%s]"
        .formatted(producerInput.producerId(), producerInput.initiativeId()));
    }

    return ProducersInitiative.builder()
      .id(producerInput.producerId() + "_" + producerInput.initiativeId())
      .producerId(producerInput.producerId())
      .producerEmail(producerInput.producerEmail())
      .producerName(producerInput.producerName())
      .initiativeId(producerInput.initiativeId())
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
    return StringUtils.isNotBlank(email) && EMAIL_VALIDATION_PATTERN.matcher(email).matches()
      ? email
      : null;
  }

  private LocalDateTime parseJsonDate(String value, String fieldName) {
    try {
      return OffsetDateTime.parse(value).toLocalDateTime();
    } catch (DateTimeParseException offsetDateTimeException) {
      try {
        return LocalDateTime.parse(value);
      } catch (DateTimeParseException ex) {
        ex.addSuppressed(offsetDateTimeException);
        throw new IllegalArgumentException("Invalid date field [%s]".formatted(fieldName), ex);
      }
    }
  }

  private record ProducerInput(String producerId, String initiativeId, String producerEmail, String producerName) {
  }
}
