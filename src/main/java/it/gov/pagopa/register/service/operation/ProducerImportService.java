package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.ProducerImportJsonDTO;
import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.dto.operation.UpdatedOperativeEmailResult;
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
import java.util.*;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.UploadEmailKeyConstant.EMAIL_INITATIVE_ERROR_KEY;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.UploadEmailKeyConstant.EMAIL_WRONG_ERROR_KEY;
import static it.gov.pagopa.register.constants.ValidationPatterns.EMAIL_PATTERN;
import static java.util.regex.Pattern.matches;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProducerImportService {

  private static final String CSV_SOURCE = "CSV";
  private static final int SAVE_BATCH_SIZE = 1000;
  private static final String INITIATIVE_START_DATE = "initiativeStartDate";
  private static final String INITIATIVE_END_DATE = "initiativeEndDate";

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
      .map(producerImportJsonDTO -> toProducer(producerImportJsonDTO, now))
      .toList();
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

  private ProducerImportJsonDTO toJsonDTO(Map<String, Object> producerFields) {
    ProducerImportJsonDTO dto = new ProducerImportJsonDTO();
    dto.setProducerId(stringValue(producerFields.get("producerId")));
    dto.setInitiativeId(stringValue(producerFields.get("initiativeId")));
    dto.setInitiativeName(stringValue(producerFields.get("initiativeName")));
    dto.setInitiativeStatus(stringValue(producerFields.get("initiativeStatus"), producerFields.get("InitiativeStatus")));
    dto.setOperativeEmail(stringValue(producerFields.get("operativeEmail")));
    dto.setInitiativeStartDate(stringValue(producerFields.get(INITIATIVE_START_DATE)));
    dto.setInitiativeEndDate(stringValue(producerFields.get(INITIATIVE_END_DATE)));
    dto.setInitiativeServiceId(stringValue(producerFields.get("initiativeServiceId")));
    dto.setInitiativeOrganizationName(stringValue(producerFields.get("initiativeOrganizationName")));
    return dto;
  }

  private String stringValue(Object... values) {
    return Arrays.stream(values)
      .filter(Objects::nonNull)
      .findFirst()
      .map(Object::toString)
      .orElse(null);
  }

  private ProducersInitiative toProducer(ProducerImportJsonDTO dto, LocalDateTime now) {
    String producerId = requiredValue(dto.getProducerId(), "producerId");
    String initiativeId = requiredValue(dto.getInitiativeId(), "initiativeId");

    String validatedEmail = null;
    if (dto.getOperativeEmail() != null && !dto.getOperativeEmail().isBlank()) {
      if (matches(EMAIL_PATTERN, dto.getOperativeEmail())) {
        validatedEmail = dto.getOperativeEmail();
      } else {
        log.warn("[IMPORT_PRODUCERS] - Invalid email format [{}] for producer {} and initiative {}. Saving producer WITHOUT email.",
          dto.getOperativeEmail(), producerId, initiativeId);
      }
    }

    return ProducersInitiative.builder()
      .id(producerId + "_" + initiativeId)
      .producerId(producerId)
      .initiativeId(initiativeId)
      .initiativeName(requiredValue(dto.getInitiativeName(), "initiativeName"))
      .initiativeStatus(parseInitiativeStatus(requiredValue(dto.getInitiativeStatus(), "initiativeStatus")))
      .operativeEmail(validatedEmail)
      .initiativeStartDate(parseJsonDate(
        requiredValue(dto.getInitiativeStartDate(), INITIATIVE_START_DATE), INITIATIVE_START_DATE))
      .initiativeEndDate(parseJsonDate(
        requiredValue(dto.getInitiativeEndDate(), INITIATIVE_END_DATE), INITIATIVE_END_DATE))
      .initiativeServiceId(requiredValue(dto.getInitiativeServiceId(), "initiativeServiceId"))
      .initiativeOrganizationName(requiredValue(dto.getInitiativeOrganizationName(), "initiativeOrganizationName"))
      .source(CSV_SOURCE)
      .enabled(Boolean.TRUE)
      .createdAt(now)
      .updatedAt(now)
      .build();
  }

  public UpdatedOperativeEmailResult updateOperativeEmail(String organizationId, String initiativeId, String newEmail) {
    String initiativeKey = organizationId + "_" + initiativeId;

    log.info("[UPDATE_OPERATIVE_EMAIL] - Request to update operative email for key: {}", initiativeKey);

    if (!matches(EMAIL_PATTERN, newEmail)) {
      log.warn("[UPDATE_OPERATIVE_EMAIL] - Provided email [{}] has an invalid format for key: {}", newEmail, initiativeKey);
      return UpdatedOperativeEmailResult.ko(EMAIL_WRONG_ERROR_KEY);
    }

    Optional<ProducersInitiative> optional = producersInitiativeRepository.findById(initiativeKey);
      if(optional.isEmpty()){
        log.error("[UPDATE_OPERATIVE_EMAIL] - Producer initiative not found for key: {}", initiativeKey);
        return UpdatedOperativeEmailResult.ko(EMAIL_INITATIVE_ERROR_KEY);
      }

    ProducersInitiative initiative = optional.get();

    initiative.setOperativeEmail(newEmail);
    initiative.setUpdatedAt(LocalDateTime.now());

    producersInitiativeRepository.save(initiative);

    log.info("[UPDATE_OPERATIVE_EMAIL] - Operative email updated successfully to [{}] for key: {}", newEmail, initiativeKey);
    return UpdatedOperativeEmailResult.ok();
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
    } catch (DateTimeParseException offsetDateTimeException) {
      try {
        return LocalDateTime.parse(value);
      } catch (DateTimeParseException ex) {
        ex.addSuppressed(offsetDateTimeException);
        throw new IllegalArgumentException("Invalid date field [%s]".formatted(fieldName), ex);
      }
    }
  }
}
