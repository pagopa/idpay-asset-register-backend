package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.dto.operation.InitiativeStatus;
import it.gov.pagopa.register.dto.operation.ProducerImportJsonDTO;
import it.gov.pagopa.register.dto.operation.ProducerImportResultDTO;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProducerImportService {

  private static final String CSV_SOURCE = "CSV";
  private static final Set<String> REQUIRED_HEADERS = Set.of(
    "producerId",
    "initiativeId",
    "initiativeName",
    "initiativeStatus",
    "initiativeStartDate",
    "initiativeEndDate",
    "initiativeServiceId",
    "initiativeOrganizationName"
  );
  private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");

  private final ProducersInitiativeRepository producersInitiativeRepository;
  private final ObjectMapper objectMapper;

  public ProducerImportResultDTO importCsv(MultipartFile csv) {
    log.info("[IMPORT_PRODUCERS] - Importing producers from file: {}", csv.getOriginalFilename());

    try {
      List<ProducersInitiative> producers = parseCsv(csv);
      producersInitiativeRepository.saveAll(producers);

      log.info("[IMPORT_PRODUCERS] - Imported {} producer records", producers.size());
      return ProducerImportResultDTO.builder()
        .status("OK")
        .importedRecords(producers.size())
        .build();
    } catch (IOException e) {
      log.error("[IMPORT_PRODUCERS] - Error reading csv file", e);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CSV file", e);
    } catch (IllegalArgumentException e) {
      log.warn("[IMPORT_PRODUCERS] - Invalid csv content: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }

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

    return records.stream()
      .map(this::toProducer)
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
    dto.setId(stringValue(record.get("_id")));
    dto.setProducerId(stringValue(record.get("producerId")));
    dto.setInitiativeId(stringValue(record.get("initiativeId")));
    dto.setInitiativeName(stringValue(record.get("initiativeName")));
    dto.setInitiativeStatus(stringValue(record.get("initiativeStatus"), record.get("InitiativeStatus")));
    dto.setInitiativeStartDate(stringValue(record.get("initiativeStartDate")));
    dto.setInitiativeEndDate(stringValue(record.get("initiativeEndDate")));
    dto.setInitiativeServiceId(stringValue(record.get("initiativeServiceId")));
    dto.setInitiativeOrganizationName(stringValue(record.get("initiativeOrganizationName")));
    dto.setEnabled(booleanValue(record.get("enabled")));
    dto.setSource(stringValue(record.get("source")));
    dto.setCreatedAt(stringValue(record.get("createdAt")));
    dto.setUpdatedAt(stringValue(record.get("updatedAt"), record.get("updateAt")));
    return dto;
  }

  private String stringValue(Object... values) {
    return Arrays.stream(values)
      .filter(value -> value != null)
      .findFirst()
      .map(Object::toString)
      .orElse(null);
  }

  private Boolean booleanValue(Object value) {
    if (value instanceof Boolean booleanValue) {
      return booleanValue;
    }
    if (value instanceof String stringValue) {
      return Boolean.valueOf(stringValue);
    }
    return null;
  }

  private List<ProducersInitiative> parseCsv(MultipartFile csv) throws IOException {
    char delimiter = detectDelimiter(csv);

    try (Reader reader = newUtf8Reader(csv);
         CSVParser parser = CSVFormat.Builder.create()
           .setHeader()
           .setSkipHeaderRecord(true)
           .setTrim(true)
           .setDelimiter(delimiter)
           .build()
           .parse(reader)) {

      validateHeaders(parser.getHeaderNames());
      LocalDateTime now = LocalDateTime.now();

      return parser.getRecords().stream()
        .map(record -> toProducer(record, now))
        .toList();
    }
  }

  private char detectDelimiter(MultipartFile csv) throws IOException {
    try (BufferedReader reader = new BufferedReader(newUtf8Reader(csv))) {
      String headerLine = reader.readLine();
      if (headerLine == null || headerLine.isBlank()) {
        throw new IllegalArgumentException("CSV file is empty");
      }
      long commaCount = headerLine.chars().filter(c -> c == ',').count();
      long semicolonCount = headerLine.chars().filter(c -> c == ';').count();
      return commaCount >= semicolonCount ? ',' : ';';
    }
  }

  private void validateHeaders(List<String> headers) {
    if (!headers.containsAll(REQUIRED_HEADERS)) {
      throw new IllegalArgumentException("CSV headers are invalid");
    }
  }

  private Reader newUtf8Reader(MultipartFile csv) throws IOException {
    PushbackReader reader = new PushbackReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8), 1);
    int firstChar = reader.read();
    if (firstChar != '\uFEFF' && firstChar != -1) {
      reader.unread(firstChar);
    }
    return reader;
  }

  private ProducersInitiative toProducer(CSVRecord record, LocalDateTime now) {
    String producerId = requiredValue(record, "producerId");
    String initiativeId = requiredValue(record, "initiativeId");

    return ProducersInitiative.builder()
      .id(producerId + "_" + initiativeId)
      .producerId(producerId)
      .initiativeId(initiativeId)
      .initiativeName(requiredValue(record, "initiativeName"))
      .initiativeStatus(parseInitiativeStatus(requiredValue(record, "initiativeStatus")))
      .initiativeStartDate(parseCsvDate(
        requiredValue(record, "initiativeStartDate"), "initiativeStartDate", record.getRecordNumber()))
      .initiativeEndDate(parseCsvDate(
        requiredValue(record, "initiativeEndDate"), "initiativeEndDate", record.getRecordNumber()))
      .initiativeServiceId(requiredValue(record, "initiativeServiceId"))
      .initiativeOrganizationName(requiredValue(record, "initiativeOrganizationName"))
      .source(CSV_SOURCE)
      .enabled(Boolean.TRUE)
      .createdAt(now)
      .updatedAt(now)
      .build();
  }

  private ProducersInitiative toProducer(ProducerImportJsonDTO dto) {
    String producerId = requiredValue(dto.getProducerId(), "producerId");
    String initiativeId = requiredValue(dto.getInitiativeId(), "initiativeId");
    String expectedId = producerId + "_" + initiativeId;
    String id = dto.getId() == null || dto.getId().isBlank() ? expectedId : dto.getId();

    if (!expectedId.equals(id)) {
      throw new IllegalArgumentException("Invalid _id. Expected [%s]".formatted(expectedId));
    }

    return ProducersInitiative.builder()
      .id(id)
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
      .source(requiredValue(dto.getSource(), "source"))
      .enabled(requiredValue(dto.getEnabled(), "enabled"))
      .createdAt(parseJsonDate(requiredValue(dto.getCreatedAt(), "createdAt"), "createdAt"))
      .updatedAt(parseJsonDate(requiredValue(dto.getUpdatedAt(), "updatedAt"), "updatedAt"))
      .build();
  }

  private String requiredValue(CSVRecord record, String fieldName) {
    String value = record.get(fieldName);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
        "Missing required field [%s] at row [%d]".formatted(fieldName, record.getRecordNumber()));
    }
    return value;
  }

  private String requiredValue(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing required field [%s]".formatted(fieldName));
    }
    return value;
  }

  private Boolean requiredValue(Boolean value, String fieldName) {
    if (value == null) {
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

  private LocalDateTime parseCsvDate(String value, String fieldName, long rowNumber) {
    try {
      return LocalDate.parse(value, CSV_DATE_FORMATTER).atStartOfDay();
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Invalid date field [%s] at row [%d]".formatted(fieldName, rowNumber), e);
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
