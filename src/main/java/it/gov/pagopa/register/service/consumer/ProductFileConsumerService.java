package it.gov.pagopa.register.service.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.kafka.BaseKafkaConsumer;
import it.gov.pagopa.register.connector.notification.NotificationServiceImpl;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.service.validator.EprelProductValidatorService;
import it.gov.pagopa.register.utils.EprelResult;
import it.gov.pagopa.register.dto.operation.StorageEventDTO;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.utils.CsvUtils;
import it.gov.pagopa.register.utils.EventDetails;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.constants.enums.UploadCsvStatus.*;
import static it.gov.pagopa.register.constants.enums.UploadCsvStatus.EPREL_ERROR;
import static it.gov.pagopa.register.model.operation.mapper.ProductMapper.mapCookingHobToProduct;

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
                                       NotificationServiceImpl notificationService){
    super(applicationName);
    this.productRepository = productRepository;
    this.fileStorageClient = fileStorageClient;
    this.objectReader = objectMapper.readerFor(new TypeReference<List<StorageEventDTO>>() {});
    this.productFileRepository = productFileRepository;
    this.eprelProductValidator = eprelProductValidator;
    this.notificationService = notificationService;
  }

  @Override
  protected ObjectReader getObjectReader() {
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

    return true;
  }

  private void processEvent(StorageEventDTO event) {
    String subject = event.getSubject();
    String url = event.getData().getUrl();

    log.info("[PRODUCT_UPLOAD] - Processing event - Subject: {}, URL: {}", subject, url);

    EventDetails eventDetails = parseEventSubject(subject);
    if (eventDetails == null) {
      return;
    }

    String blobPath = extractBlobPath(url);
    if (blobPath == null) {
      return;
    }

    processFileFromStorage(blobPath, url, eventDetails);
  }

  private EventDetails parseEventSubject(String subject) {
    Matcher matcher = SUBJECT_PATTERN.matcher(subject);
    if (!matcher.find() || matcher.groupCount() < 3) {
      log.warn("[PRODUCT_UPLOAD] - Invalid subject format: {}", subject);
      return null;
    }

    String orgId = matcher.group(1).trim();
    String category = matcher.group(2);
    String productFileId =  matcher.group(3).replace(".csv","");
    log.info("[PRODUCT_UPLOAD] - Processing fileId: {} for orgId={}, category={}", productFileId, orgId, category);

    return new EventDetails(orgId, category, productFileId);
  }

  private String extractBlobPath(String url) {
    int pathStart = url.indexOf("/CSV/");
    if (pathStart == -1) {
      log.error("[PRODUCT_UPLOAD] - Unable to extract file path from URL: {}", url);
      return null;
    }
    return url.substring(pathStart + 1);
  }

  private void processFileFromStorage(String blobPath, String url, EventDetails eventDetails) {
    try (ByteArrayOutputStream downloadedData = fileStorageClient.download(blobPath)) {
      if (downloadedData == null) {
        log.warn("[PRODUCT_UPLOAD] - File not found or download failed for path: {} (from URL: {})", blobPath, url);
        setProductFileStatus(eventDetails.getProductFileId(), String.valueOf(EPREL_ERROR),0);
        return;
      }

      processCsvFromStorage(downloadedData, eventDetails.getProductFileId(), eventDetails.getCategory(), eventDetails.getOrgId());

    } catch (Exception e) {
      log.error("[PRODUCT_UPLOAD] - Error processing file {}: {}", eventDetails.getProductFileId(), e.getMessage(), e);
      setProductFileStatus(eventDetails.getProductFileId(), String.valueOf(EPREL_ERROR),0);
    }
  }

  public void processCsvFromStorage(ByteArrayOutputStream byteArrayOutputStream,
                                    String fileId,
                                    String category,
                                    String orgId) {

    try {
      boolean isCookingHob = COOKINGHOBS.equalsIgnoreCase(category);
      setProductFileStatus(fileId, String.valueOf(IN_PROCESS),0);
      List<String> headers = CsvUtils.readHeader(byteArrayOutputStream);
      List<CSVRecord> records = CsvUtils.readCsvRecords(byteArrayOutputStream);
      log.info("[PRODUCT_UPLOAD] - Valid CSV headers: {}", headers);
      if (isCookingHob) {
        processCookingHobRecords(records, orgId, fileId);
      } else {
        EprelResult validationResult = eprelProductValidator.validateRecords(records, EPREL_FIELDS, category, orgId, fileId);
        processEprelResult(validationResult.getValidRecords(), validationResult.getInvalidRecords(), validationResult.getErrorMessages(), fileId, headers,category);
      }
    } catch (Exception e) {
      log.error("[UPLOAD_PRODUCT_FILE] - Generic Error ", e);
    }
  }

  private void processCookingHobRecords(List<CSVRecord> records, String orgId, String productFileId) {
    List<Product> result = new ArrayList<>();
    for (CSVRecord csvRecord : records) {
      result.add(mapCookingHobToProduct(csvRecord, orgId, productFileId));
      log.info("[PRODUCT_UPLOAD] - Added cooking hob product: {}", csvRecord.get(CODE_PRODUCT));
    }
    if (!result.isEmpty()) {
      List<Product> savedProduct = productRepository.saveAll(result);
      log.info("[PRODUCT_UPLOAD] - Saved {} valid products for file {}", savedProduct.size(), productFileId);
      setProductFileStatus(productFileId, String.valueOf(LOADED),savedProduct.size());
      log.info("[PRODUCT_UPLOAD] - File {} processed successfully with no errors", productFileId);
      notificationService.sendEmailOk(COOKINGHOBS+"_"+productFileId+".csv",null);
    }
  }

  private void processEprelResult(List<Product> validProduct, List<CSVRecord> errors, Map<CSVRecord, String> messages, String productFileId, List<String> headers, String category) {
    if (!errors.isEmpty()) {
      processErrorRecords(errors, messages, productFileId, headers);
      setProductFileStatus(productFileId, String.valueOf(EPREL_ERROR),validProduct.size());
      log.info("[PRODUCT_UPLOAD] - File {} processed with {} EPREL errors", productFileId, errors.size());
      notificationService.sendEmailPartial(category+"_"+productFileId+".csv", null);
    }
    else if (!validProduct.isEmpty()) {
      List<Product> savedProduct =productRepository.saveAll(validProduct);
      log.info("[PRODUCT_UPLOAD] - Saved {} valid products for file {}", savedProduct.size(), productFileId);
      setProductFileStatus(productFileId, String.valueOf(LOADED),savedProduct.size());
      log.info("[PRODUCT_UPLOAD] - File {} processed successfully with no errors", productFileId);
      notificationService.sendEmailOk(category+"_"+productFileId+".csv",null);
    }
  }

  private void processErrorRecords(List<CSVRecord> errors, Map<CSVRecord, String> messages, String productFileId, List<String> headers) {
    try {
      String errorFileName = FilenameUtils.getBaseName(productFileId + ".csv");
      CsvUtils.writeCsvWithErrors(errors, headers, messages, errorFileName);
      Path tempFilePath = Paths.get("/tmp/", errorFileName);
      String destination = "Report/Eprel_Error/" + productFileId + ".csv";
      fileStorageClient.upload(Files.newInputStream(tempFilePath), destination, "text/csv");
    } catch (Exception e) {
      log.error("[UPLOAD_PRODUCT_FILE] - Generic Error ", e);
    }
  }

  private void setProductFileStatus(String fileId, String status, int added) {
    Optional<ProductFile> productFile = productFileRepository.findById(fileId);
    productFile.ifPresent(file -> {
      file.setUploadStatus(status);
      file.setAddedProductNumber(added);
      productFileRepository.save(file);
      log.info("[PRODUCT_UPLOAD] - Final status for file {} set to: {}", fileId, status);
    });
  }


}
