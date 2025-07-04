package it.gov.pagopa.register.service.operation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.kafka.BaseKafkaConsumer;
import it.gov.pagopa.register.connector.eprel.EprelConnector;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.constants.AssetRegisterConstant;
import it.gov.pagopa.register.constants.enums.UploadCsvStatus;
import it.gov.pagopa.register.dto.operation.*;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.mapper.operation.ProductFileMapper;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.utils.CsvUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;

import static it.gov.pagopa.register.constants.RegisterConstants.CsvRecord.*;
import static it.gov.pagopa.register.constants.RegisterConstants.*;
import static it.gov.pagopa.register.constants.enums.UploadCsvStatus.*;

@Slf4j
@Service
public class ProductFileService extends BaseKafkaConsumer<List<StorageEventDTO>> {

  private final ProductFileRepository productFileRepository;
  private final ProductRepository productRepository;
  private final EprelConnector eprelConnector;
  private final FileStorageClient fileStorageClient;
  private final ObjectReader objectReader;
  private final ProductFileValidator productFileValidator;

  public ProductFileService(ProductFileRepository productFileRepository,
                            ProductRepository productRepository,
                            FileStorageClient fileStorageClient,
                            ObjectMapper objectMapper,
                            @Value("${spring.application.name}") String applicationName,
                            EprelConnector eprelConnector,
                            ProductFileValidator productFileValidator) {
    super(applicationName);
    this.productFileRepository = productFileRepository;
    this.productFileValidator = productFileValidator;
    this.productRepository = productRepository;
    this.eprelConnector = eprelConnector;
    this.fileStorageClient = fileStorageClient;
    this.objectReader = objectMapper.readerFor(new TypeReference<List<StorageEventDTO>>() {});
  }

  public ProductFileResponseDTO getFilesByPage(String organizationId, Pageable pageable) {

    if (organizationId == null) {
      throw new IllegalArgumentException("OrganizationId must not be null");
    }

    Page<ProductFile> filesPage = productFileRepository.findByOrganizationIdAndUploadStatusNot(
      organizationId, UploadCsvStatus.FORMAL_ERROR.name(), pageable);

    Page<ProductFileDTO> filesPageDTO = filesPage.map(ProductFileMapper::toDTO);

    return ProductFileResponseDTO.builder()
      .content(filesPageDTO.getContent())
      .pageNo(filesPageDTO.getNumber())
      .pageSize(filesPageDTO.getSize())
      .totalElements(filesPageDTO.getTotalElements())
      .totalPages(filesPageDTO.getTotalPages())
      .build();
  }

  public FileReportDTO downloadReport(String id, String organizationId) {
    ProductFile productFile = productFileRepository.findByIdAndOrganizationId(id, organizationId)
      .orElseThrow(() -> new ReportNotFoundException("Report not found with id: " + id));

    String filePath;

    if (AssetRegisterConstant.EPREL_ERROR.equals(productFile.getUploadStatus())) {
      filePath = "Report/Eprel_Error/" + productFile.getId() + ".csv";
    } else if (AssetRegisterConstant.FORMAL_ERROR.equals(productFile.getUploadStatus())) {
      filePath = "Report/Formal_Error/" + productFile.getId() + ".csv";
    } else {
      throw new ReportNotFoundException("Report not available for file: " + productFile.getFileName());
    }

    ByteArrayOutputStream result = fileStorageClient.download(filePath);

    if (result == null) {
      throw new ReportNotFoundException("Report not found on Azure for path: " + filePath);
    }

    return FileReportDTO.builder().data(result.toByteArray()).filename(FilenameUtils.getBaseName(productFile.getFileName()) + "_errors.csv").build();
  }

  public ProductFileResult processFile(MultipartFile file, String category, String organizationId, String userId) {

    try {

      String originalFileName = file.getOriginalFilename();
      List<String> headers = CsvUtils.readHeader(file);
      List<CSVRecord> records = CsvUtils.readCsvRecords(file);

      //TODO check if for the specified organization there are some file uploaded or in progress and stop the upload of new file

      ValidationResultDTO validation = productFileValidator.validateFile(file, category, headers, records.size());
      if ("KO".equals(validation.getStatus())) {
        return ProductFileResult.ko(validation.getErrorKey());
      }

      ValidationResultDTO result = productFileValidator.validateRecords(records, headers, category);

      if (result != null && !CollectionUtils.isEmpty(result.getInvalidRecords())) {
        String errorFileName = FilenameUtils.getBaseName(file.getOriginalFilename()) + "_errors.csv";
        CsvUtils.writeCsvWithErrors(result.getInvalidRecords(), headers, result.getErrorMessages(), errorFileName);

        //TODO verify if really needed
        ProductFile productFile = productFileRepository.save(ProductFile.builder()
          .fileName(originalFileName)
          .uploadStatus(FORMAL_ERROR.name())
          .category(category)
          .findedProductsNumber(records.size())
          .addedProductNumber(NumberUtils.INTEGER_ZERO)
          .userId(userId)
          .organizationId(organizationId)
          .dateUpload(LocalDateTime.now())
          .build());

        Path tempFilePath = Paths.get("/tmp/", errorFileName);
        String destination = "Report/Formal_Error/" + productFile.getId() + ".csv";
        fileStorageClient.upload(Files.newInputStream(tempFilePath), destination, file.getContentType());

        return ProductFileResult.ko(AssetRegisterConstant.UploadKeyConstant.REPORT_FORMAL_FILE_ERROR_KEY, productFile.getId());
      }


      // Log OK
      ProductFile productFile = productFileRepository.save(ProductFile.builder()
        .fileName(originalFileName)
        .uploadStatus(UPLOADED.name())
        .category(category)
        .findedProductsNumber(records.size())
        .addedProductNumber(NumberUtils.INTEGER_ZERO)
        .userId(userId)
        .organizationId(organizationId)
        .dateUpload(LocalDateTime.now())
        .build());

      // Upload on Azure
      fileStorageClient.upload(file.getInputStream(), "CSV/"+organizationId+"/"+category+"/"+productFile.getId()+".csv", file.getContentType());

      return ProductFileResult.ok();

    } catch (Exception e) {
      log.error("[UPLOAD_PRODUCT_FILE] - Generic Error ", e);
      return ProductFileResult.ko("GENERIC_ERROR");
    }
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
    String fileName = matcher.group(3);

    log.info("[PRODUCT_UPLOAD] - Processing file: {} for orgId={}, category={}", fileName, orgId, category);

    return new EventDetails(orgId, category, fileName);
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
    ByteArrayOutputStream downloadedData = null;
    try {
      downloadedData = fileStorageClient.download(blobPath);
      if (downloadedData == null) {
        log.warn("[PRODUCT_UPLOAD] - File not found or download failed for path: {} (from URL: {})", blobPath, url);
        setProductFileStatus(eventDetails.getFileName(), String.valueOf(EPREL_ERROR));
        return;
      }

      processCsvFromStorage(downloadedData, eventDetails.getFileName(), eventDetails.getCategory(), eventDetails.getOrgId());

    } catch (Exception e) {
      log.error("[PRODUCT_UPLOAD] - Error processing file {}: {}", eventDetails.getFileName(), e.getMessage(), e);
      setProductFileStatus(eventDetails.getFileName(), String.valueOf(EPREL_ERROR));
    } finally {
      if (downloadedData != null) {
        try {
          downloadedData.close();
        } catch (IOException e) {
          log.warn("[PRODUCT_UPLOAD] - Error closing ByteArrayOutputStream for file {}: {}", eventDetails.getFileName(), e.getMessage());
        }
      }
    }
  }

  public void processCsvFromStorage(ByteArrayOutputStream byteArrayOutputStream,
                                    String fileName,
                                    String category,
                                    String orgId) {
    CsvProcessingResult result = new CsvProcessingResult();
    boolean isCookingHob = CATEGORY_COOKINGHOBS.equalsIgnoreCase(category);
    String productFileId = fileName.replace(".csv", "");
    setProductFileStatus(fileName, String.valueOf(UPLOADED));

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
      new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), StandardCharsets.UTF_8));
         CSVParser csvParser = new CSVParser(reader, CSVFormat.Builder.create()
           .setHeader()
           .setTrim(true)
           .setDelimiter(';')
           .build())) {

      List<CSVRecord> records = csvParser.getRecords();
      Set<String> headers = new LinkedHashSet<>(csvParser.getHeaderMap().keySet());
      log.info("[PRODUCT_UPLOAD] - Valid CSV headers: {}", headers);

      for (CSVRecord csvRecord : records) {
        List<String> errors = processCsvRecord(csvRecord, orgId, category, productFileId, result.getValidProducts(), isCookingHob);
        if (!errors.isEmpty()) {
          result.addErrorRow(buildErrorRow(csvRecord, headers, errors));
        }
      }

      handleProcessingResults(result, fileName, headers);

    } catch (IOException e) {
      log.error("[PRODUCT_UPLOAD] - Error reading CSV {}: {}", fileName, e.getMessage());
      setProductFileStatus(fileName, String.valueOf(EPREL_ERROR));
    }
  }

  private void handleProcessingResults(CsvProcessingResult result, String fileName, Set<String> headers) {
    if (!result.getValidProducts().isEmpty()) {
      productRepository.saveAll(result.getValidProducts());
      log.info("[PRODUCT_UPLOAD] - Saved {} valid products for file {}", result.getValidProducts().size(), fileName);
    }

    if (!result.getErrorRows().isEmpty()) {
      generateErrorReport(fileName, result.getErrorRows(), headers);
      setProductFileStatus(fileName, String.valueOf(EPREL_ERROR));
      log.info("[PRODUCT_UPLOAD] - File {} processed with {} EPREL errors", fileName, result.getErrorRows().size());
    } else {
      setProductFileStatus(fileName, String.valueOf(LOADED));
      log.info("[PRODUCT_UPLOAD] - File {} processed successfully with no errors", fileName);
    }
  }

  private List<String> processCsvRecord(CSVRecord csvRecord, String orgId, String category, String productFileId,
                                        List<Product> validProducts, boolean isCookingHob) {
    if (isCookingHob) {
      return processCookingHobRecord(csvRecord, orgId, productFileId, validProducts);
    }

    return processEprelRecord(csvRecord, orgId, category, productFileId, validProducts);
  }

  private List<String> processCookingHobRecord(CSVRecord csvRecord, String orgId, String productFileId,
                                               List<Product> validProducts) {
    validProducts.add(mapCookingHobToProduct(csvRecord, orgId, productFileId));
    log.debug("[PRODUCT_UPLOAD] - Added cooking hob product: {}", csvRecord.get(PRODUCT_CODE));
    return new ArrayList<>();
  }

  private List<String> processEprelRecord(CSVRecord csvRecord, String orgId, String category, String productFileId,
                                          List<Product> validProducts) {
    List<String> errors = new ArrayList<>();

    try {
      String eprelCode = csvRecord.get(EPREL_CODE);
      EprelProductDTO eprelData = eprelConnector.callEprel(eprelCode);
      log.info("[PRODUCT_UPLOAD] - EPREL response for {}: {}", eprelCode, eprelData);

      List<String> eprelErrors = validateEprelData(eprelData, category);

      if (eprelErrors.isEmpty()) {
        validProducts.add(mapEprelToProduct(csvRecord, eprelData, orgId, productFileId,category));
        log.debug("[PRODUCT_UPLOAD] - EPREL product validated: {}", eprelCode);
      } else {
        errors.addAll(eprelErrors);
      }

    } catch (Exception e) {
      log.error("[PRODUCT_UPLOAD] - EPREL error for code {}: {}", csvRecord.get(EPREL_CODE), e.getMessage());
      errors.add("EPREL call error: " + e.getMessage());
    }

    return errors;
  }

  private List<String> buildErrorRow(CSVRecord csvRecord, Set<String> headers, List<String> errors) {
    List<String> row = new ArrayList<>();
    headers.forEach(h -> row.add(csvRecord.get(h)));
    row.add(String.join(", ", errors));
    return row;
  }

  private void setProductFileStatus(String fileName, String status) {
    String fileId = fileName.replace(".csv", "");
    Optional<ProductFile> productFile = productFileRepository.findById(fileId);
    if (productFile.isPresent()) {
      productFile.get().setUploadStatus(status);
      productFileRepository.save(productFile.get());
      log.info("[PRODUCT_UPLOAD] - Final status for file {} set to: {}", fileName, status);
    }
  }

  private Product mapCookingHobToProduct(CSVRecord csvRecord, String orgId, String productFileId) {
    return Product.builder()
      .productFileId(productFileId)
      .organizationId(orgId)
      .registrationDate(LocalDateTime.now())
      .status(STATUS_APPROVED)
      .productCode(csvRecord.get(PRODUCT_CODE))
      .gtinCode(csvRecord.get(GTIN_EAN_CODE))
      .category(CATEGORY_COOKINGHOBS)
      .countryOfProduction(csvRecord.get(PRODUCTION_COUNTRY))
      .brand(csvRecord.get(BRAND))
      .model(csvRecord.get(MODEL))
      .build();
  }

  private Product mapEprelToProduct(CSVRecord csvRecord, EprelProductDTO eprelData, String orgId, String productFileId, String category) {
    return Product.builder()
      .productFileId(productFileId)
      .organizationId(orgId)
      .registrationDate(LocalDateTime.now())
      .status(STATUS_APPROVED)
      .productCode(csvRecord.get(PRODUCT_CODE))
      .gtinCode(csvRecord.get(GTIN_EAN_CODE))
      .eprelCode(csvRecord.get(EPREL_CODE))
      .category(category)
      .productGroup(eprelData.getProductGroup())
      .countryOfProduction(csvRecord.get(PRODUCTION_COUNTRY))
      .brand(eprelData.getSupplierOrTrademark())
      .model(eprelData.getModelIdentifier())
      .energyClass(eprelData.getEnergyClass())
      .build();
  }

  private List<String> validateEprelData(EprelProductDTO eprelData, String expectedCategory) {
    List<String> errors = new ArrayList<>();

    if (eprelData == null) {
      errors.add("Product not found in EPREL");
      return errors;
    }

    validateVerificationStatuses(eprelData, errors);
    validateProductStatusAndBlocking(eprelData, errors);
    validateProductGroup(eprelData, expectedCategory, errors);
    validateEnergyClassCompliance(eprelData, expectedCategory, errors);

    return errors;
  }

  private void validateVerificationStatuses(EprelProductDTO eprelData, List<String> errors) {
    if (!"VERIFIED".equalsIgnoreCase(eprelData.getOrgVerificationStatus())) {
      errors.add("orgVerificationStatus is not VERIFIED");
    }

    if (!"VERIFIED".equalsIgnoreCase(eprelData.getTrademarkVerificationStatus())) {
      errors.add("trademarkVerificationStatus is not VERIFIED");
    }
  }

  private void validateProductStatusAndBlocking(EprelProductDTO eprelData, List<String> errors) {
    if (Boolean.TRUE.equals(eprelData.getBlocked())) {
      errors.add("Product is blocked");
    }

    if (!"PUBLISHED".equalsIgnoreCase(eprelData.getStatus())) {
      errors.add("Status is not PUBLISHED");
    }
  }

  private void validateProductGroup(EprelProductDTO eprelData, String expectedCategory, List<String> errors) {
    if (eprelData.getProductGroup() == null ||
      !eprelData.getProductGroup().toLowerCase().startsWith(expectedCategory.toLowerCase())) {
      errors.add("Product group from EPREL is not compatible with expected category");
    }
  }

  private void validateEnergyClassCompliance(EprelProductDTO eprelData, String expectedCategory, List<String> errors) {
    if (!isEnergyClassValid(eprelData.getEnergyClass(), expectedCategory)) {
      String requiredClass = ENERGY_CLASS_REQUIREMENTS.get(expectedCategory.toUpperCase());
      errors.add(String.format("Energy class %s is not compliant. Minimum required: %s",
        eprelData.getEnergyClass(), requiredClass));
    }
  }

  private boolean isEnergyClassValid(String energyClass, String category) {
    if (energyClass == null || energyClass.isBlank()) {
      return false;
    }

    String requiredMinClass = ENERGY_CLASS_REQUIREMENTS.get(category.toUpperCase());
    if (requiredMinClass == null) {
      log.warn("[PRODUCT_UPLOAD] - Unrecognized category for energy class validation: {}", category);
      return false;
    }

    int requiredIndex = ENERGY_CLASS_ORDER.indexOf(requiredMinClass);
    int productIndex = ENERGY_CLASS_ORDER.indexOf(energyClass.toUpperCase());

    if (requiredIndex == -1 || productIndex == -1) {
      log.warn("[PRODUCT_UPLOAD] - Unrecognized energy class. Required: {}, Product: {}",
        requiredMinClass, energyClass);
      return false;
    }

    return productIndex <= requiredIndex;
  }

  private void generateErrorReport(String fileName,
                                   List<List<String>> errorRows,
                                   Set<String> headers) {
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         BufferedWriter writer = new BufferedWriter(
           new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8));
         CSVPrinter csvPrinter = new CSVPrinter(writer,
           CSVFormat.Builder.create()
             .setHeader(headers.toArray(new String[0]))
             .setTrim(true)
             .setDelimiter(';')
             .build())) {

      log.info("[PRODUCT_UPLOAD] - Writing EPREL error rows: {}", errorRows);

      for (List<String> row : errorRows) {
        csvPrinter.printRecord(row);
      }

      csvPrinter.flush();
      writer.flush();

      ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

      String destination = "Report/Eprel_Error/" + fileName;

      fileStorageClient.upload(inputStream, destination, "text/csv");

      log.info("[PRODUCT_UPLOAD] - EPREL error report uploaded to {}", destination);

    } catch (IOException e) {
      log.error("[PRODUCT_UPLOAD] - Error generating EPREL error report for file {}: {}", fileName, e.getMessage(), e);
    }
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

  @Getter
  private static class EventDetails {
    private final String orgId;
    private final String category;
    private final String fileName;

    public EventDetails(String orgId, String category, String fileName) {
      this.orgId = orgId;
      this.category = category;
      this.fileName = fileName;
    }

  }

  @Getter
  private static class CsvProcessingResult {
    private final List<Product> validProducts = new ArrayList<>();
    private final List<List<String>> errorRows = new ArrayList<>();

    public void addErrorRow(List<String> errorRow) { this.errorRows.add(errorRow); }
  }
}
