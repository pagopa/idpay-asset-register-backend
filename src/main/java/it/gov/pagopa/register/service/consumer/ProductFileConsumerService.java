package it.gov.pagopa.register.service.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.kafka.BaseKafkaConsumer;
import it.gov.pagopa.register.connector.notification.NotificationServiceImpl;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.dto.operation.StorageEventDTO;
import it.gov.pagopa.register.dto.utils.EprelResult;
import it.gov.pagopa.register.dto.utils.EventDetails;
import it.gov.pagopa.register.enums.ProductStatusEnum;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.service.validator.EprelProductValidatorService;
import it.gov.pagopa.register.utils.CsvUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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
import static it.gov.pagopa.register.mapper.operation.ProductMapper.mapCookingHobToProduct;
import static it.gov.pagopa.register.mapper.operation.ProductMapper.mapProductToCsvRow;

@Slf4j
@Service
public class ProductFileConsumerService extends BaseKafkaConsumer<List<StorageEventDTO>> {

  private final ProductRepository productRepository;
  private final ObjectReader objectReader;
  private final ProductFileRepository productFileRepository;
  private final FileStorageClient fileStorageClient;
  private final EprelProductValidatorService eprelProductValidator;
  private final NotificationServiceImpl notificationService;

  protected ProductFileConsumerService(@Value("${spring.application.name}") String applicationName,
                                       ProductRepository productRepository,
                                       FileStorageClient fileStorageClient,
                                       ObjectMapper objectMapper,
                                       ProductFileRepository productFileRepository,
                                       EprelProductValidatorService eprelProductValidator,
                                       NotificationServiceImpl notificationService) {
    super(applicationName);
    this.productRepository = productRepository;
    this.fileStorageClient = fileStorageClient;
    this.objectReader = objectMapper.readerFor(new TypeReference<List<StorageEventDTO>>() {
    });
    this.productFileRepository = productFileRepository;
    this.eprelProductValidator = eprelProductValidator;
    this.notificationService = notificationService;
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
    events.stream()
      .filter(this::isValidEvent)
      .forEach(this::processEvent);
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
    if (!matcher.find() || matcher.groupCount() < 3) {
      log.warn("[PRODUCT_UPLOAD] - Invalid subject format: {}", subject);
      return null;
    }

    String orgId = matcher.group(1).trim();
    String category = matcher.group(2);
    String productFileId = matcher.group(3).replace(".csv", "");
    log.info("[PRODUCT_UPLOAD] - Processing fileId: {} for orgId={}, category={}", productFileId, orgId, category);

    return new EventDetails(orgId, category, productFileId);
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

  private void processFileFromStorage(String blobPath, String url, EventDetails eventDetails) {
    try (ByteArrayOutputStream downloadedData = fileStorageClient.download(blobPath)) {
      if (downloadedData == null) {
        log.warn("[PRODUCT_UPLOAD] - File not found or download failed for path: {} (from URL: {})", blobPath, url);
        setProductFileStatus(eventDetails.getProductFileId(), String.valueOf(PARTIAL), 0);
        return;
      }

      log.info("[PRODUCT_UPLOAD] - File downloaded successfully from path: {}", blobPath);
      processCsvFromStorage(downloadedData, eventDetails.getProductFileId(), eventDetails.getCategory(), eventDetails.getOrgId());

    } catch (Exception e) {
      log.error("[PRODUCT_UPLOAD] - Error processing file {}: {}", eventDetails.getProductFileId(), e.getMessage(), e);
      setProductFileStatus(eventDetails.getProductFileId(), String.valueOf(PARTIAL), 0);
    }
  }

  public void processCsvFromStorage(ByteArrayOutputStream byteArrayOutputStream,
                                    String fileId,
                                    String category,
                                    String orgId) {

    try {
      boolean isCookingHob = COOKINGHOBS.equalsIgnoreCase(category);
      setProductFileStatus(fileId, String.valueOf(IN_PROCESS), 0);
      List<String> headers = CsvUtils.readHeader(byteArrayOutputStream);
      List<CSVRecord> records = CsvUtils.readCsvRecords(byteArrayOutputStream);
      log.info("[PRODUCT_UPLOAD] - Valid CSV headers: {}", headers);
      if (isCookingHob) {
        processCookingHobRecords(records, orgId, fileId,headers);
      } else {
        EprelResult validationResult = eprelProductValidator.validateRecords(records, EPREL_FIELDS, category, orgId, fileId, headers);
        processEprelResult(validationResult.getValidRecords().values().stream().toList(), validationResult.getInvalidRecords(), validationResult.getErrorMessages(), fileId, headers, category);
      }
    } catch (Exception e) {
      log.error("[UPLOAD_PRODUCT_FILE] - Generic Error ", e);
    }
  }

  private void processCookingHobRecords(List<CSVRecord> records, String orgId, String productFileId, List<String> headers) {
    Map<String,Product> validProduct = new LinkedHashMap<>();
    List<CSVRecord> invalidRecords = new ArrayList<>();
    Map<CSVRecord, String> errorMessages = new HashMap<>();
    for (CSVRecord csvRecord : records) {
      Optional<Product> optProduct = productRepository.findById(csvRecord.get(CODE_GTIN_EAN));
      boolean isProductPresent = optProduct.isPresent();
      boolean dbCheck = true;
      if(isProductPresent){
        if( !orgId.equals(optProduct.get().getOrganizationId())){
          invalidRecords.add(csvRecord);
          errorMessages.put(csvRecord,DUPLICATE_GTIN_EAN_WITH_DIFFERENT_ORGANIZATIONID);
          dbCheck = false;
        } else if (!ProductStatusEnum.APPROVED.toString().equals(optProduct.get().getStatus())) {
          invalidRecords.add(csvRecord);
          errorMessages.put(csvRecord,DUPLICATE_GTIN_EAN_WITH_STATUS_NOT_APPROVED);
          dbCheck = false;
        }
      }
      if(dbCheck){
        if(validProduct.containsKey(csvRecord.get(CODE_GTIN_EAN))) {
          Product duplicateGtin = validProduct.remove(csvRecord.get(CODE_GTIN_EAN));
          CSVRecord duplicateGtinRow = mapProductToCsvRow(duplicateGtin,COOKINGHOBS, headers);
          invalidRecords.add(duplicateGtinRow);
          errorMessages.put(duplicateGtinRow,DUPLICATE_GTIN_EAN);
          log.info("[PRODUCT_UPLOAD] - Duplicate error for record with GTIN code: {}", csvRecord.get(CODE_GTIN_EAN));
        }
        validProduct.put(csvRecord.get(CODE_GTIN_EAN),mapCookingHobToProduct(csvRecord, orgId, productFileId));
        log.info("[PRODUCT_UPLOAD] - Added cooking hob product: {}", csvRecord.get(CODE_GTIN_EAN));

      }

    }
    processCookingHoobsRecords(productFileId, headers, validProduct, invalidRecords, errorMessages);
  }

  private void processCookingHoobsRecords(String productFileId, List<String> headers, Map<String, Product> validProduct, List<CSVRecord> invalidRecords, Map<CSVRecord, String> errorMessages) {
    if (!validProduct.isEmpty()) {
      List<Product> savedProduct = productRepository.saveAll(validProduct.values().stream().toList());
      log.info("[PRODUCT_UPLOAD] - Saved {} valid products for file {}", savedProduct.size(), productFileId);
      if (!invalidRecords.isEmpty()) {
        processErrorRecords(invalidRecords, errorMessages, productFileId, headers);
        String userEmail = setProductFileStatus(productFileId, String.valueOf(PARTIAL), validProduct.size());
        log.info("[PRODUCT_UPLOAD] - File {} processed with {} duplicate rows", productFileId, errorMessages.size());
        notificationService.sendEmailPartial(COOKINGHOBS + "_" + productFileId + ".csv", userEmail);
      } else {
        String userEmail = setProductFileStatus(productFileId, String.valueOf(LOADED), savedProduct.size());
        log.info("[PRODUCT_UPLOAD] - File {} processed successfully with no errors", productFileId);
        notificationService.sendEmailOk(COOKINGHOBS + "_" + productFileId + ".csv", userEmail);
      }
    } else if (!invalidRecords.isEmpty()) {
      processErrorRecords(invalidRecords, errorMessages, productFileId, headers);
      String userEmail = setProductFileStatus(productFileId, String.valueOf(PARTIAL), 0);
      log.info("[PRODUCT_UPLOAD] - File {} processed with {} duplicate row", productFileId, invalidRecords.size());
      notificationService.sendEmailPartial(COOKINGHOBS + "_" + productFileId + ".csv", userEmail);
    }
  }

  private void processEprelResult(List<Product> validProduct, List<CSVRecord> errors, Map<CSVRecord, String> messages, String productFileId, List<String> headers, String category) {
    if (!validProduct.isEmpty()) {
      List<Product> savedProduct = productRepository.saveAll(validProduct);
      log.info("[PRODUCT_UPLOAD] - Saved {} valid products for file {}", savedProduct.size(), productFileId);
      if (!errors.isEmpty()) {
        processErrorRecords(errors, messages, productFileId, headers);
        String userEmail = setProductFileStatus(productFileId, String.valueOf(PARTIAL), validProduct.size());
        log.info("[PRODUCT_UPLOAD] - File {} processed with {} EPREL errors", productFileId, errors.size());
        notificationService.sendEmailPartial(category + "_" + productFileId + ".csv", userEmail);
      } else {
        String userEmail = setProductFileStatus(productFileId, String.valueOf(LOADED), savedProduct.size());
        log.info("[PRODUCT_UPLOAD] - File {} processed successfully with no errors", productFileId);
        notificationService.sendEmailOk(category + "_" + productFileId + ".csv", userEmail);
      }
    } else if (!errors.isEmpty()) {
      processErrorRecords(errors, messages, productFileId, headers);
      String userEmail = setProductFileStatus(productFileId, String.valueOf(PARTIAL), 0);
      log.info("[PRODUCT_UPLOAD] - File {} processed with {} EPREL errors", productFileId, errors.size());
      notificationService.sendEmailPartial(category + "_" + productFileId + ".csv", userEmail);
    }
  }


  @SuppressWarnings("java:S5443") //The system used will be Linux so never create a file without specified permissions
  private void processErrorRecords(List<CSVRecord> errors, Map<CSVRecord, String> messages, String productFileId, List<String> headers) {
    try {
      Path tempFilePath;
      if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
        tempFilePath = Files.createTempFile("errors-", ".csv", attr);
      } else {
        tempFilePath = Files.createTempFile("errors-", ".csv");
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
