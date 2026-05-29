package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.initiative.PortalInitiativeService;
import it.gov.pagopa.register.dto.operation.InitiativeDTO;
import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.dto.operation.ProducerInitiativeRequestDTO;
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
        log.warn("[IMPORT_PRODUCERS] - Skipping producerId [{}], initiativeId [{}]. error={}",
          producerId(request), initiativeId(request), e.getMessage());
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

  private ProducerInput toProducerInput(ProducerInitiativeRequestDTO request) {
    if (request == null) {
      throw new IllegalArgumentException("Producer request cannot be null");
    }

    return new ProducerInput(
      requiredValue(request.getProducerId(), "producerId"),
      requiredValue(request.getInitiativeId(), INITIATIVE_ID),
      requiredValue(request.getProducerName(), "producerName"),
      optionalEmail(request.getProducerEmail())
    );
  }

  private ProducersInitiative toProducer(ProducerInput producerInput, InitiativeDTO initiativeDetail, LocalDateTime now) {
    if (initiativeDetail == null) {
      throw new IllegalArgumentException("Initiative detail not found for producerId [%s] and initiativeId [%s]"
        .formatted(producerInput.producerId(), producerInput.initiativeId()));
    }

    return ProducersInitiative.builder()
      .id(producerInput.producerId() + "_" + producerInput.initiativeId())
      .producerId(producerInput.producerId())
      .producerName(producerInput.producerName())
      .producerEmail(producerInput.producerEmail())
      .initiativeId(producerInput.initiativeId())
      .initiativeName(requiredValue(initiativeDetail.getInitiativeName(), "initiativeName"))
      .initiativeStatus(requiredStatus(initiativeDetail.getStatus(), "initiativeStatus"))
      .initiativeStartDate(requiredDate(requiredGeneral(initiativeDetail).getStartDate(), "initiativeStartDate").atStartOfDay())
      .initiativeEndDate(requiredDate(requiredGeneral(initiativeDetail).getEndDate(), "initiativeEndDate").atStartOfDay())
      .initiativeServiceId(requiredValue(requiredAdditionalInfo(initiativeDetail).getServiceId(), "initiativeServiceId"))
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
