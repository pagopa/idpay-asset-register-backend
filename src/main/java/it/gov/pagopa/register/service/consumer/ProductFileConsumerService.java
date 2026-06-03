package it.gov.pagopa.register.service.consumer;

import com.azure.storage.blob.models.BlobStorageException;
import it.gov.pagopa.common.kafka.BaseKafkaConsumer;
import it.gov.pagopa.register.model.initiative.CategoryConfig;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import it.gov.pagopa.register.configuration.InitiativeConfigMap;
import it.gov.pagopa.register.connector.notification.NotificationServiceImpl;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.dto.operation.StorageEventDTO;
import it.gov.pagopa.register.dto.utils.EventDetails;
import it.gov.pagopa.register.dto.utils.ProductValidationResult;
import it.gov.pagopa.register.event.producer.ProductFileProducer;
import it.gov.pagopa.register.exception.operation.EprelException;
import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProducersInitiativeRepository;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.service.validator.product.ValidationService;
import it.gov.pagopa.register.utils.CsvUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

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
  private final ValidationService validationService;
  private final NotificationServiceImpl notificationService;
  private final ProductFileProducer productFileProducer;
  private final ConsumerControlService consumerControlService;
  private final InitiativeConfigMap initiativeConfigMap;
  private final ObjectMapper objectMapper;
  private final ProducersInitiativeRepository producersInitiativeRepository;
  protected ProductFileConsumerService(@Value("${spring.application.name}") String applicationName,
                                       ProductRepository productRepository,
                                       FileStorageClient fileStorageClient,
                                       ObjectMapper objectMapper,
                                       ProductFileRepository productFileRepository,
                                       ValidationService validationService, NotificationServiceImpl notificationService,
                                       ProductFileProducer productFileProducer,
                                       ConsumerControlService consumerControlService,
                                       InitiativeConfigMap initiativeConfigMap,
                                       ProducersInitiativeRepository producersInitiativeRepository){
    super(applicationName);
    this.productRepository = productRepository;
    this.fileStorageClient = fileStorageClient;
    this.objectReader = objectMapper.readerFor(new TypeReference<List<StorageEventDTO>>() {});
    this.productFileRepository = productFileRepository;
    this.validationService = validationService;
    this.notificationService = notificationService;
    this.productFileProducer = productFileProducer;
    this.objectMapper = objectMapper;
    this.consumerControlService = consumerControlService;
    this.initiativeConfigMap = initiativeConfigMap;
    this.producersInitiativeRepository = producersInitiativeRepository;
  }

  @Override
  protected ObjectReader getObjectReader() {
    log.info("[PRODUCT_UPLOAD] - Getting ObjectReader");
    return objectReader;
  }

  @Override
  protected void onDeserializationError(Message<@NonNull String> message, Throwable e) {
    log.error("[PRODUCT_UPLOAD] - Deserialization error: {}", e.getMessage(), e);
  }

  @Override
  protected void onError(Message<@NonNull String> message, Throwable e) {
    log.error("[PRODUCT_UPLOAD] - Unexpected error: {}", e.getMessage(), e);
  }

  @Override
  public void execute(List<StorageEventDTO> events, Message<@NonNull String> message) {
    log.info("[PRODUCT_UPLOAD] - Executing with {} events", events.size());

    List<StorageEventDTO> toRetry = new ArrayList<>();

    for (StorageEventDTO event : events) {
      if (isValidEvent(event)) {
        try {
          processEvent(event);
        } catch (EprelException _) {
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
      boolean sent = productFileProducer.scheduleMessage(json);
      if(!sent){
        unlockFile(events);
      }
    } catch (JacksonException e) {
      unlockFile(events);
      log.error("JsonProcessingException: {}", e.getMessage());
    }
  }

  private void unlockFile(List<StorageEventDTO> events) {
    for (StorageEventDTO event : events) {
      String subject = event.getSubject();
      EventDetails eventDetails = parseEventSubject(subject);
      setProductFileStatus(eventDetails.getProductFileId(), String.valueOf(PARTIAL), 0);
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
    if (!matcher.find() || matcher.groupCount() < 5) {
      log.warn("[PRODUCT_UPLOAD] - Invalid subject format: {}", subject);
      return null;
    }

    String initiativeId = matcher.group(1).trim();
    String orgId = matcher.group(2);
    String organizationName = matcher.group(3);
    String category = matcher.group(4);
    String productFileId = matcher.group(5).replace(CSV, "");
    log.info("[PRODUCT_UPLOAD] - Processing fileId: {} for initiativeId={} ,orgId={}, category={}, organizationName={}", productFileId, initiativeId, orgId, category,organizationName);

    return new EventDetails(orgId, category, productFileId,organizationName, initiativeId);
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
    processCsvFromStorage(downloadedData, eventDetails.getProductFileId(), eventDetails.getCategory(), eventDetails.getOrgId(), eventDetails.getOrganizationName(), eventDetails.getInitiativeId());
    }

  public void processCsvFromStorage(ByteArrayOutputStream byteArrayOutputStream,
                                    String fileId,
                                    String category,
                                    String orgId,
                                    String organizationName,
                                    String initiativeId) {

    try {
      String initiativeKey = orgId + "_" + initiativeId;
      Optional<ProducersInitiative> initiativeOpt = producersInitiativeRepository.findById(initiativeKey);
      if (initiativeOpt.isEmpty() || !Boolean.TRUE.equals(initiativeOpt.get().getEnabled())) {
        log.warn("[PROCESS_FILE] - Organization {} is not enabled or not found for initiative: {}", orgId, initiativeId);
        setProductFileStatus(fileId, String.valueOf(PARTIAL), 0);
      }

      String userEmail = null;
      if (initiativeOpt.isPresent() && (initiativeOpt.get().getProducerEmail() == null || initiativeOpt.get().getProducerEmail().isBlank())) {
        log.warn("[PROCESS_FILE] - Upload blocked. Missing operative email for key: {}", initiativeKey);
        setProductFileStatus(fileId, String.valueOf(PARTIAL), 0);
      } else {
        userEmail = initiativeOpt.get().getProducerEmail();
      }

      setProductFileStatus(fileId, String.valueOf(IN_PROCESS), 0);

      List<String> headers = CsvUtils.readHeader(byteArrayOutputStream);
      List<CSVRecord> records = CsvUtils.readCsvRecords(byteArrayOutputStream);

      log.info("[PRODUCT_UPLOAD] - Valid CSV headers: {}", headers);

      InitiativeConfig initiativeConfig = initiativeConfigMap.get(initiativeId);
      List<String> allowedReloadStatuses = initiativeConfig.getAllowedReloadStatuses();
      CategoryConfig categoryConfig = initiativeConfig.getCategories().get(category);

      ProductValidationResult validationResult =
        validationService.validateRecords(
          records,
          category,
          orgId,
          initiativeId,
          fileId,
          headers,
          organizationName,
          initiativeConfig,
          categoryConfig,
          allowedReloadStatuses
        );

      processResult(
        validationResult.getValidRecords().values().stream().toList(),
        validationResult.getInvalidRecords(),
        validationResult.getErrorMessages(),
        initiativeId,
        fileId,
        userEmail,
        headers,
        category
      );

    } catch (IOException e) {
      log.error("[UPLOAD_PRODUCT_FILE] - Error while reading CSV", e);
      setProductFileStatus(fileId, String.valueOf(PARTIAL), 0);
    }
  }

  private void processResult(List<Product> validProduct, List<CSVRecord> errors, Map<CSVRecord, String> messages,
                             String initiativeId, String productFileId, String userEmail, List<String> headers, String category) {

    int savedCount = 0;

    if (!validProduct.isEmpty()) {
      List<Product> savedProduct = productRepository.saveAll(validProduct);
      savedCount = savedProduct.size();
      log.info("[PRODUCT_UPLOAD] - Saved {} valid products for file {}", savedCount, productFileId);
    }

    if (!errors.isEmpty()) {
      handleErrors(errors, messages, initiativeId, productFileId, headers, userEmail, category, savedCount);
    } else if (savedCount > 0) {
      setProductFileStatus(productFileId, String.valueOf(LOADED), savedCount);
      log.info("[PRODUCT_UPLOAD] - File {} processed successfully with no errors", productFileId);

      String fileName = CATEGORIES_FOR_FILENAME.get(category) + "_" + productFileId + CSV;
      notificationService.sendEmailOk(fileName, userEmail);
    }
  }

  private void handleErrors(List<CSVRecord> errors, Map<CSVRecord, String> messages, String initiativeId,
                            String productFileId, List<String> headers, String userEmail, String category, int savedCount) {
    processErrorRecords(errors, messages, initiativeId, productFileId, headers);
    setProductFileStatus(productFileId, String.valueOf(PARTIAL), savedCount);
    log.info("[PRODUCT_UPLOAD] - File {} processed with {} errors", productFileId, errors.size());

    String fileName = CATEGORIES_FOR_FILENAME.get(category) + "_" + productFileId + CSV;
    notificationService.sendEmailPartial(fileName, userEmail);
  }

  @SuppressWarnings("java:S5443") //The system used will be Linux so never create a file without specified permissions
  private void processErrorRecords(List<CSVRecord> errors, Map<CSVRecord, String> messages, String initiativeId, String productFileId, List<String> headers) {
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
      String destination = REPORT_PARTIAL_ERROR + initiativeId + "/" + productFileId + CSV;
      fileStorageClient.upload(Files.newInputStream(tempFilePath), destination, "text/csv");
      Files.deleteIfExists(tempFilePath);
      log.info("[PRODUCT_UPLOAD] - Error file uploaded to {}", destination);
    } catch (Exception e) {
      log.error("[UPLOAD_PRODUCT_FILE] - Generic Error ", e);
    }
  }

  protected void setProductFileStatus(String fileId, String status, int added) {
    Optional<ProductFile> productFileOptional = productFileRepository.findById(fileId);

    if (productFileOptional.isPresent()) {
      ProductFile productFile = productFileOptional.get();
      productFile.setUploadStatus(status);
      productFile.setAddedProductNumber(added);
      productFileRepository.save(productFile);
      log.info("[PRODUCT_UPLOAD] - Final status for file {} set to: {}", fileId, status);
    } else {
      log.warn("[PRODUCT_UPLOAD] - No product file found with id: {}", fileId);
    }
  }

}
