package it.gov.pagopa.register.service.consumer;

import com.azure.storage.blob.models.BlobStorageException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.kafka.BaseKafkaConsumer;
import it.gov.pagopa.register.connector.notification.NotificationServiceImpl;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.dto.operation.StorageEventDTO;
import it.gov.pagopa.register.dto.utils.EventDetails;
import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.event.producer.ProductFileProducer;
import it.gov.pagopa.register.exception.operation.EprelException;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.service.validator.CookinghobsValidatorService;
import it.gov.pagopa.register.service.validator.EprelProductValidatorService;
import it.gov.pagopa.register.utils.CsvUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.regex.Matcher;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.enums.UploadCsvStatus.*;

@Slf4j
@Service
public class ProductFileConsumerService extends BaseKafkaConsumer<List<StorageEventDTO>> {

  private final ProductRepository productRepository;
  private final ObjectReader objectReader;
  private final ProductFileRepository productFileRepository;
  private final FileStorageClient fileStorageClient;
  private final EprelProductValidatorService eprelProductValidator;
  private final CookinghobsValidatorService cookinghobsValidatorService;
  private final NotificationServiceImpl notificationService;
  private final ProductFileProducer productFileProducer;

  private final ConsumerControlService consumerControlService;
  private final ObjectMapper objectMapper;
  protected ProductFileConsumerService(@Value("${spring.application.name}") String applicationName,
                                       ProductRepository productRepository,
                                       FileStorageClient fileStorageClient,
                                       ObjectMapper objectMapper,
                                       ProductFileRepository productFileRepository,
                                       EprelProductValidatorService eprelProductValidator,
                                       CookinghobsValidatorService cookinghobsValidatorService,
                                       NotificationServiceImpl notificationService,
                                       ProductFileProducer productFileProducer,
                                       ConsumerControlService consumerControlService){
    super(applicationName);
    this.productRepository = productRepository;
    this.fileStorageClient = fileStorageClient;
    this.objectReader = objectMapper.readerFor(new TypeReference<List<StorageEventDTO>>() {});
    this.productFileRepository = productFileRepository;
    this.eprelProductValidator = eprelProductValidator;
    this.cookinghobsValidatorService = cookinghobsValidatorService;
    this.notificationService = notificationService;
    this.productFileProducer = productFileProducer;
    this.objectMapper = objectMapper;
    this.consumerControlService = consumerControlService;
  }

  @Override
  protected ObjectReader getObjectReader() {
    log.info("[PRODUCT_UPLOAD] - Getting ObjectReader");
    return objectReader;
  }

  @Override
  protected void onDeserializationError(Message<String> message, Throwable e) {
    log.error("[PRODUCT_UPLOAD] - Deserialization error: {}", e.getMessage(), e);
  }

  @Override
  protected void onError(Message<String> message, Throwable e) {
    log.error("[PRODUCT_UPLOAD] - Unexpected error: {}", e.getMessage(), e);
  }

  @Override
  public void execute(List<StorageEventDTO> events, Message<String> message) {
    log.info("[PRODUCT_UPLOAD] - Executing with {} events", events.size());

    List<StorageEventDTO> toRetry = new ArrayList<>();

    for (StorageEventDTO event : events) {
      if (isValidEvent(event)) {
        try {
          processEvent(event);
        } catch (EprelException e) {
          toRetry.add(event);
        }
      }
    }
    if (!toRetry.isEmpty()) {
        consumerControlService.stopConsumer();
        retryLater(toRetry);
        consumerControlService.startEprelHealthCheck();
      }
  }

  private void retryLater(List<StorageEventDTO> events) {
    try {
      String json = objectMapper.writeValueAsString(events);
      productFileProducer.scheduleMessage(json);
    } catch (JsonProcessingException e) {
      for (StorageEventDTO event : events) {
        String subject = event.getSubject();
        EventDetails eventDetails = parseEventSubject(subject);
        if (eventDetails == null) {
          log.warn("[PRODUCT_UPLOAD] - Event details are null, skipping event");
          return;
        }
        setProductFileStatus(eventDetails.getProductFileId(), String.valueOf(PARTIAL), 0);
      }
      log.error("JsonProcessingException: {}", e.getMessage());
    }
  }


  private boolean isValidEvent(StorageEventDTO event) {
    if (event == null || event.getData() == null) {
      log.warn("[PRODUCT_UPLOAD] - Null event or event data, skipping");
      return false;
    }

    String url = event.getData().getUrl();
    if (url == null || url.trim().isEmpty()) {
      log.warn("[PRODUCT_UPLOAD] - Empty or null URL in event, skipping. Subject: {}", event.getSubject());
      return false;
    }

    log.info("[PRODUCT_UPLOAD] - Valid event: {}", event.getSubject());
    return true;
  }

  private void processEvent(StorageEventDTO event) {
    String subject = event.getSubject();
    String url = event.getData().getUrl();

    log.info("[PRODUCT_UPLOAD] - Processing event - Subject: {}, URL: {}", subject, url);

    EventDetails eventDetails = parseEventSubject(subject);
    if (eventDetails == null) {
      log.warn("[PRODUCT_UPLOAD] - Event details are null, skipping event");
      return;
    }

    String blobPath = extractBlobPath(url);
    if (blobPath == null) {
      log.warn("[PRODUCT_UPLOAD] - Blob path is null, skipping event");
      return;
    }

    processFileFromStorage(blobPath, url, eventDetails);
  }

  protected EventDetails parseEventSubject(String subject) {
    Matcher matcher = SUBJECT_PATTERN.matcher(subject);
    if (!matcher.find() || matcher.groupCount() < 4) {
      log.warn("[PRODUCT_UPLOAD] - Invalid subject format: {}", subject);
      return null;
    }

    String orgId = matcher.group(1).trim();
    String organizationName = matcher.group(2);
    String category = matcher.group(3);
    String productFileId = matcher.group(4).replace(CSV, "");
    log.info("[PRODUCT_UPLOAD] - Processing fileId: {} for orgId={}, category={}, organizationName={}", productFileId, orgId, category,organizationName);

    return new EventDetails(orgId, category, productFileId,organizationName);
  }

  protected String extractBlobPath(String url) {
    int pathStart = url.indexOf("/CSV/");
    if (pathStart == -1) {
      log.error("[PRODUCT_UPLOAD] - Unable to extract file path from URL: {}", url);
      return null;
    }
    log.info("[PRODUCT_UPLOAD] - Extracted blob path from URL: {}", url);
    return url.substring(pathStart + 1);
  }

  private void processFileFromStorage(String blobPath, String url, EventDetails eventDetails) throws EprelException{
    ByteArrayOutputStream downloadedData;
    try {
        downloadedData = fileStorageClient.download(blobPath);
        if (downloadedData == null) {
          log.warn("[PRODUCT_UPLOAD] - File not found or download failed for path: {} (from URL: {})", blobPath, url);
          setProductFileStatus(eventDetails.getProductFileId(), String.valueOf(PARTIAL), 0);
          return;
      }
    } catch (BlobStorageException e){
      log.error("[PRODUCT_UPLOAD] - Azure Storage Error: {}",e.getMessage());
      setProductFileStatus(eventDetails.getProductFileId(), String.valueOf(PARTIAL), 0);
      return;
    }
    log.info("[PRODUCT_UPLOAD] - File downloaded successfully from path: {}", blobPath);
    processCsvFromStorage(downloadedData, eventDetails.getProductFileId(), eventDetails.getCategory(), eventDetails.getOrgId(), eventDetails.getOrganizationName());
    }

  public void processCsvFromStorage(ByteArrayOutputStream byteArrayOutputStream,
                                    String fileId,
                                    String category,
                                    String orgId,
                                    String organizationName) {

    try {
      boolean isCookingHob = COOKINGHOBS.equalsIgnoreCase(category);
      setProductFileStatus(fileId, String.valueOf(IN_PROCESS), 0);
      List<String> headers = CsvUtils.readHeader(byteArrayOutputStream);
      List<CSVRecord> records = CsvUtils.readCsvRecords(byteArrayOutputStream);
      log.info("[PRODUCT_UPLOAD] - Valid CSV headers: {}", headers);
      ProductValidationResult validationResult;
      if (isCookingHob) {
        validationResult = cookinghobsValidatorService.validateRecords(records, orgId, fileId,headers,organizationName);
      } else {
        validationResult = eprelProductValidator.validateRecords(records, EPREL_FIELDS, category, orgId, fileId, headers,organizationName);
      }
      processResult(validationResult.getValidRecords().values().stream().toList(), validationResult.getInvalidRecords(), validationResult.getErrorMessages(), fileId, headers, category);
    } catch (IOException e) {
      log.error("[UPLOAD_PRODUCT_FILE] - Error while reading CSV", e);
      setProductFileStatus(fileId, String.valueOf(PARTIAL), 0);
    }
  }

  private void processResult(List<Product> validProduct, List<CSVRecord> errors, Map<CSVRecord, String> messages, String productFileId, List<String> headers, String category) {
    if (!validProduct.isEmpty()) {
      List<Product> savedProduct = productRepository.saveAll(validProduct);
      log.info("[PRODUCT_UPLOAD] - Saved {} valid products for file {}", savedProduct.size(), productFileId);
      if (!errors.isEmpty()) {
        processErrorRecords(errors, messages, productFileId, headers);
        String userEmail = setProductFileStatus(productFileId, String.valueOf(PARTIAL), validProduct.size());
        log.info("[PRODUCT_UPLOAD] - File {} processed with {} errors", productFileId, errors.size());
        notificationService.sendEmailPartial(CATEGORIES_TO_IT_P.get(category) + "_" + productFileId + CSV, userEmail);
      } else {
        String userEmail = setProductFileStatus(productFileId, String.valueOf(LOADED), savedProduct.size());
        log.info("[PRODUCT_UPLOAD] - File {} processed successfully with no errors", productFileId);
        notificationService.sendEmailOk(CATEGORIES_TO_IT_P.get(category)  + "_" + productFileId + CSV, userEmail);
      }
    } else if (!errors.isEmpty()) {
      processErrorRecords(errors, messages, productFileId, headers);
      String userEmail = setProductFileStatus(productFileId, String.valueOf(PARTIAL), 0);
      log.info("[PRODUCT_UPLOAD] - File {} processed with {} errors", productFileId, errors.size());
      notificationService.sendEmailPartial(CATEGORIES_TO_IT_P.get(category)  + "_" + productFileId + CSV, userEmail);
    }
  }

  @SuppressWarnings("java:S5443") //The system used will be Linux so never create a file without specified permissions
  private void processErrorRecords(List<CSVRecord> errors, Map<CSVRecord, String> messages, String productFileId, List<String> headers) {
    try {
      Path tempFilePath;
      if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
        tempFilePath = Files.createTempFile("errors-", CSV, attr);
      } else {
        tempFilePath = Files.createTempFile("errors-", CSV);
      }
      CsvUtils.writeCsvWithErrors(errors, headers, messages,  tempFilePath);
      String destination = REPORT_PARTIAL_ERROR + productFileId + CSV;
      fileStorageClient.upload(Files.newInputStream(tempFilePath), destination, "text/csv");
      Files.deleteIfExists(tempFilePath);
      log.info("[PRODUCT_UPLOAD] - Error file uploaded to {}", destination);
    } catch (Exception e) {
      log.error("[UPLOAD_PRODUCT_FILE] - Generic Error ", e);
    }
  }

  protected String setProductFileStatus(String fileId, String status, int added) {
    Optional<ProductFile> productFileOptional = productFileRepository.findById(fileId);

    if (productFileOptional.isPresent()) {
      ProductFile productFile = productFileOptional.get();
      productFile.setUploadStatus(status);
      productFile.setAddedProductNumber(added);
      productFileRepository.save(productFile);
      log.info("[PRODUCT_UPLOAD] - Final status for file {} set to: {}", fileId, status);
      return productFile.getUserEmail();
    } else {
      log.warn("[PRODUCT_UPLOAD] - No product file found with id: {}", fileId);
      return null;
    }
  }

}
