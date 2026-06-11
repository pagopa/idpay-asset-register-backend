package it.gov.pagopa.register.service.operation;

import feign.FeignException;
import it.gov.pagopa.register.connector.initiative.PortalInitiativeService;
import it.gov.pagopa.register.dto.operation.*;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.UploadEmailError.EMAIL_INITATIVE_ERROR_KEY;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.UploadEmailError.EMAIL_WRONG_ERROR_KEY;
import static it.gov.pagopa.register.constants.ValidationPatterns.EMAIL_PATTERN;

import static java.util.regex.Pattern.matches;

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
  private final PortalInitiativeService portalInitiativeService;

  public ProducerImportResultDTO importProducers(List<ProducerInitiativeRequestDTO> requests) {
    log.info("[IMPORT_PRODUCERS] - Importing producers from request payload");

    try {
      ProducerConversionResult conversionResult = toProducers(requests);
      ProducerImportResultDTO result = saveInBatches(
        conversionResult.producers(),
        conversionResult.totalRecords(),
        conversionResult.failedRecords()
      );

      log.info("[IMPORT_PRODUCERS] - Import completed. totalRecords={}, importedRecords={}, failedRecords={}",
        result.getTotalRecords(), result.getImportedRecords(), result.getFailedRecords());
      return result;
    } catch (IllegalArgumentException e) {
      log.warn("[IMPORT_PRODUCERS] - Invalid request content: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }

  private ProducerImportResultDTO saveInBatches(List<ProducersInitiative> producers, int totalRecords, int failedRecords) {
    int importedRecords = 0;
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
          start + 1, end, importedRecords, totalRecords - importedRecords - failedRecords);
      } catch (RuntimeException e) {
        failedRecords += batch.size();
        lastError = e;
        log.error("[IMPORT_PRODUCERS] - Failed saving batch {}/{} records. importedRecords={}, failedRecords={}, error={}",
          start + 1, end, importedRecords, failedRecords, e.getMessage(), e);
      }
    }

    if (lastError != null) {
      String message = "Producer import partially failed. totalRecords=%d, importedRecords=%d, failedRecords=%d"
        .formatted(totalRecords, importedRecords, failedRecords);
      HttpStatus status = isRequestTimeout(lastError) ? HttpStatus.REQUEST_TIMEOUT : HttpStatus.INTERNAL_SERVER_ERROR;
      log.error("[IMPORT_PRODUCERS] - {}", message, lastError);
      throw new ResponseStatusException(status, message, lastError);
    }

    return ProducerImportResultDTO.builder()
      .status(failedRecords > 0 ? "PARTIAL" : "OK")
      .totalRecords(totalRecords)
      .importedRecords(importedRecords)
      .failedRecords(failedRecords)
      .message(failedRecords > 0
        ? "Producer import completed with failed records"
        : "Producer import completed successfully")
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

  private ProducerConversionResult toProducers(List<ProducerInitiativeRequestDTO> requests) {
    if (requests == null || requests.isEmpty()) {
      throw new IllegalArgumentException("Producer request payload does not contain records");
    }

    LocalDateTime now = LocalDateTime.now();
    Map<String, InitiativeDTO> initiativeDetails = new HashMap<>();
    List<ProducersInitiative> producers = new ArrayList<>();
    int failedRecords = 0;

    for (ProducerInitiativeRequestDTO request : requests) {
      try {
        ProducerInput producerInput = toProducerInput(request);
        producers.add(toProducer(
          producerInput,
          initiativeDetails.computeIfAbsent(producerInput.initiativeId(), portalInitiativeService::getInitiativeDetail),
          now));
      } catch (RuntimeException e) {
        failedRecords++;
        logSkippedRecord(request, e);
      }
    }

    return new ProducerConversionResult(producers, requests.size(), failedRecords);
  }

  private String producerId(ProducerInitiativeRequestDTO request) {
    return request == null ? null : StringUtils.strip(request.getProducerId());
  }

  private String initiativeId(ProducerInitiativeRequestDTO request) {
    return request == null ? null : StringUtils.strip(request.getInitiativeId());
  }

  private void logSkippedRecord(ProducerInitiativeRequestDTO request, RuntimeException e) {
    if (e instanceof FeignException feignException) {
      log.warn("[IMPORT_PRODUCERS] - Skipping producer record. producerId=[{}], initiativeId=[{}], invalidField=[{}], portalStatus=[{}], portalError={}",
        producerId(request), initiativeId(request), invalidField(e), feignException.status(), portalError(feignException));
      return;
    }

    log.warn("[IMPORT_PRODUCERS] - Skipping producer record. producerId=[{}], initiativeId=[{}], invalidField=[{}], error={}",
      producerId(request), initiativeId(request), invalidField(e), e.getMessage());
  }

  private ProducerInput toProducerInput(ProducerInitiativeRequestDTO request) {
    if (request == null) {
      throw new IllegalArgumentException("Producer request cannot be null");
    }

    return new ProducerInput(
      requiredValue(request.getProducerId(), "producerId"),
      requiredValue(request.getInitiativeId(), INITIATIVE_ID),
      requiredValue(request.getProducerName(), "producerName"),
      optionalEmail(request.getProducerEmail(), request)
    );
  }

  private ProducersInitiative toProducer(ProducerInput producerInput, InitiativeDTO initiativeDetail, LocalDateTime now) {
    if (initiativeDetail == null) {
      throw new IllegalArgumentException("Initiative detail not found for producerId [%s] and initiativeId [%s]"
        .formatted(producerInput.producerId(), producerInput.initiativeId()));
    }
    String producerInitiativeId = producerInput.producerId() + "_" + producerInput.initiativeId();

    String validatedEmail = null;
    if (producerInput.producerEmail != null && !producerInput.producerEmail.isBlank()) {
      if (matches(EMAIL_PATTERN, producerInput.producerEmail)) {
        validatedEmail = producerInput.producerEmail;
      } else {
        log.warn("[IMPORT_PRODUCERS] - Invalid email format [{}] for producer {} and initiative {}. Saving producer WITHOUT email.",
          producerInput.producerEmail, producerInput.producerId, producerInput.initiativeId);
      }
    }

    return ProducersInitiative.builder()
      .id(producerInitiativeId)
      .producerId(producerInput.producerId())
      .producerName(producerInput.producerName())
      .producerEmail(validatedEmail)
      .initiativeId(producerInput.initiativeId())
      .initiativeName(requiredValue(initiativeDetail.getInitiativeName(), "initiativeName"))
      .initiativeStatus(requiredStatus(initiativeDetail.getStatus(), "initiativeStatus"))
      .initiativeStartDate(requiredDate(requiredGeneral(initiativeDetail).getStartDate(), "initiativeStartDate").atStartOfDay())
      .initiativeEndDate(requiredDate(requiredGeneral(initiativeDetail).getEndDate(), "initiativeEndDate").atStartOfDay())
      .initiativeServiceId(requiredValue(requiredAdditionalInfo(initiativeDetail).getServiceId(), "initiativeServiceId"))
      .initiativeOrganizationName(requiredValue(initiativeDetail.getOrganizationName(), "initiativeOrganizationName"))
      .source(CSV_SOURCE)
      .enabled(Boolean.TRUE)
      .createdAt(resolveCreatedAt(producerInitiativeId, now))
      .updatedAt(now)
      .build();
  }

  private LocalDateTime resolveCreatedAt(String producerInitiativeId, LocalDateTime now) {
    return Optional.ofNullable(producersInitiativeRepository.findById(producerInitiativeId))
      .flatMap(existingProducer -> existingProducer)
      .map(ProducersInitiative::getCreatedAt)
      .orElse(now);
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

    initiative.setProducerEmail(newEmail.toLowerCase());
    initiative.setUpdatedAt(LocalDateTime.now());

    producersInitiativeRepository.save(initiative);

    log.info("[UPDATE_OPERATIVE_EMAIL] - Operative email updated successfully to [{}] for key: {}", newEmail, initiativeKey);
    return UpdatedOperativeEmailResult.ok();
  }

  private String requiredValue(String value, String fieldName) {
    String cleanedValue = StringUtils.strip(value);
    if (StringUtils.isBlank(cleanedValue)) {
      throw new IllegalArgumentException(MISSING_REQUIRED_FIELD_MESSAGE.formatted(fieldName));
    }
    return cleanedValue;
  }

  private String optionalEmail(String value, ProducerInitiativeRequestDTO request) {
    String email = StringUtils.strip(value);
    if (StringUtils.isBlank(email)) {
      return null;
    }
    if (EMAIL_VALIDATION_PATTERN.matcher(email).matches()) {
      return email;
    }
    log.warn("[IMPORT_PRODUCERS] - Invalid producerEmail for producerId [{}], initiativeId [{}]. value will be stored as null",
      producerId(request), initiativeId(request));
    return null;
  }

  private String invalidField(RuntimeException e) {
    if (e instanceof FeignException.NotFound) {
      return INITIATIVE_ID;
    }
    String message = e.getMessage();
    if (message == null || !message.startsWith("Missing required field [")) {
      return message != null && message.toLowerCase().contains("initiative")
        ? INITIATIVE_ID
        : null;
    }
    return StringUtils.substringBetween(message, "[", "]");
  }

  private String portalError(FeignException e) {
    String responseBody = e.contentUTF8();
    return StringUtils.isNotBlank(responseBody) ? responseBody : e.getMessage();
  }

  private InitiativeDTO.InitiativeGeneralDTO requiredGeneral(InitiativeDTO initiativeDetail) {
    if (initiativeDetail.getGeneral() == null) {
      throw new IllegalArgumentException(MISSING_REQUIRED_FIELD_MESSAGE.formatted("initiativeGeneral"));
    }
    return initiativeDetail.getGeneral();
  }

  private InitiativeDTO.InitiativeAdditionalDTO requiredAdditionalInfo(InitiativeDTO initiativeDetail) {
    if (initiativeDetail.getAdditionalInfo() == null) {
      throw new IllegalArgumentException(MISSING_REQUIRED_FIELD_MESSAGE.formatted("initiativeAdditionalInfo"));
    }
    return initiativeDetail.getAdditionalInfo();
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

  private record ProducerInput(String producerId, String initiativeId, String producerName, String producerEmail) {
  }

  private record ProducerConversionResult(List<ProducersInitiative> producers, int totalRecords, int failedRecords) {
  }
}
