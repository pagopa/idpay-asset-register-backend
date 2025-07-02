package it.gov.pagopa.register.service.operation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.kafka.BaseKafkaConsumer;
import it.gov.pagopa.register.connector.eprel.EprelConnector;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.dto.operation.EprelProductDTO;
import it.gov.pagopa.register.dto.operation.ProductFileDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.dto.operation.StorageEventDTO;
import it.gov.pagopa.register.mapper.operation.ProductFileMapper;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.model.role.Product;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.role.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;

import static it.gov.pagopa.register.constants.RegisterConstants.*;
import static it.gov.pagopa.register.constants.enums.UploadCsvStatus.EPREL_ERROR;
import static it.gov.pagopa.register.constants.enums.UploadCsvStatus.LOADED;

@Slf4j
@Service
public class ProductFileService extends BaseKafkaConsumer<List<StorageEventDTO>> {

  private final ProductFileRepository productFileRepository;
  private final ProductRepository productRepository;
  private final EprelConnector eprelConnector;
  private final FileStorageClient fileStorageClient;
  private final ObjectReader objectReader;

  public ProductFileService(ProductFileRepository productFileRepository,
                            ProductRepository productRepository,
                            FileStorageClient fileStorageClient,
                            ObjectMapper objectMapper,
                            @Value("${spring.application.name}") String applicationName,
                            EprelConnector eprelConnector) {
    super(applicationName);
    this.productFileRepository = productFileRepository;
    this.productRepository = productRepository;
    this.eprelConnector = eprelConnector;
    this.fileStorageClient = fileStorageClient;
    this.objectReader = objectMapper.readerFor(new TypeReference<List<StorageEventDTO>>() {});
  }

  public ProductFileResponseDTO downloadFilesByPage(String organizationId, Pageable pageable) {
    if (organizationId == null) {
      throw new IllegalArgumentException("OrganizationId must not be null");
    }

    Page<ProductFile> filesPage = productFileRepository.findByOrganizationIdAndUploadStatusNot(
      organizationId, "FORMAL_ERROR", pageable);

    Page<ProductFileDTO> filesPageDTO = filesPage.map(ProductFileMapper::toDTO);

    return ProductFileResponseDTO.builder()
      .content(filesPageDTO.getContent())
      .pageNo(filesPageDTO.getNumber())
      .pageSize(filesPageDTO.getSize())
      .totalElements(filesPageDTO.getTotalElements())
      .totalPages(filesPageDTO.getTotalPages())
      .build();
  }


  @Override
  public void execute(List<StorageEventDTO> events, Message<String> message) {
    for (StorageEventDTO event : events) {
      if (event == null || event.getData() == null) {
        log.warn("[PRODUCT_UPLOAD] - Null event or event data, skipping");
        continue;
      }

      String subject = event.getSubject();
      String url = event.getData().getUrl();

      if (url == null || url.trim().isEmpty()) {
        log.warn("[PRODUCT_UPLOAD] - Empty or null URL in event, skipping. Subject: {}", subject);
        continue;
      }

      log.info("[PRODUCT_UPLOAD] - Processing event - Subject: {}, URL: {}", subject, url);

      Matcher matcher = SUBJECT_PATTERN.matcher(subject);
      if (!matcher.find() || matcher.groupCount() < 4) {
        log.warn("[PRODUCT_UPLOAD] - Invalid subject format: {}", subject);
        continue;
      }

      String orgId = matcher.group(1);
      String category = matcher.group(2);
      String userId = matcher.group(3);
      String fileName = matcher.group(4);

      log.info("[PRODUCT_UPLOAD] - Processing file: {} for orgId={}, category={}, userId={}",
        fileName, orgId, category, userId);

      ByteArrayOutputStream downloadedData = null;
      try {
        downloadedData = fileStorageClient.download(url);
        if (downloadedData == null) {
          log.warn("[PRODUCT_UPLOAD] - File not found or download failed for URL: {}. " +
            "This might be a timing issue or the file was already processed.", url);
          setFinalProductFileStatus(fileName, String.valueOf(EPREL_ERROR));
          continue;
        }

        processCsvFromStorage(downloadedData, fileName, category, orgId);
      } catch (Exception e) {
        log.error("[PRODUCT_UPLOAD] - Error processing file {}: {}", fileName, e.getMessage(), e);
        setFinalProductFileStatus(fileName, String.valueOf(EPREL_ERROR));
      } finally {
        if (downloadedData != null) {
          try {
            downloadedData.close();
          } catch (IOException e) {
            log.warn("[PRODUCT_UPLOAD] - Error closing ByteArrayOutputStream for file {}: {}", fileName, e.getMessage());
          }
        }
      }
    }
  }

  public void processCsvFromStorage(ByteArrayOutputStream byteArrayOutputStream,
                                    String fileName,
                                    String category,
                                    String orgId) {

    List<Product> validProducts = new ArrayList<>();
    List<List<String>> errorRows = new ArrayList<>();
    boolean isCookingHob = "COOKINGHOBS".equalsIgnoreCase(category);
    String productFileId = fileName.replace(".csv", "");

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

      for (int i = 0; i < records.size(); i++) {
        CSVRecord record = records.get(i);
        List<String> errors = processCsvRecord(record, orgId, category, productFileId, validProducts, isCookingHob);
        if (!errors.isEmpty()) {
          errorRows.add(buildErrorRow(record, headers, errors));
        }
      }

      if (!validProducts.isEmpty()) {
        productRepository.saveAll(validProducts);
        log.info("[PRODUCT_UPLOAD] - Saved {} valid products for file {}", validProducts.size(), fileName);
      }

      if (!errorRows.isEmpty()) {
        generateErrorReport(fileName, errorRows, headers);
        setFinalProductFileStatus(fileName, String.valueOf(EPREL_ERROR));
        log.info("[PRODUCT_UPLOAD] - File {} processed with {} EPREL errors", fileName, errorRows.size());
      } else {
        setFinalProductFileStatus(fileName, String.valueOf(LOADED));
        log.info("[PRODUCT_UPLOAD] - File {} processed successfully with no errors", fileName);
      }

    } catch (IOException e) {
      log.error("[PRODUCT_UPLOAD] - Error reading CSV {}: {}", fileName, e.getMessage());
      setFinalProductFileStatus(fileName, String.valueOf(EPREL_ERROR));
    }
  }

  private List<String> processCsvRecord(CSVRecord record, String orgId, String category, String productFileId,
                                        List<Product> validProducts, boolean isCookingHob) {
    List<String> errors = new ArrayList<>();

    if (isCookingHob) {
      validProducts.add(mapCookingHobToProduct(record, orgId, productFileId));
      log.debug("[PRODUCT_UPLOAD] - Added cooking hob product: {}", record.get("Codice Prodotto"));
      return errors;
    }

    try {
      EprelProductDTO eprelData = eprelConnector.callEprel(record.get("Codice EPREL"));
      List<String> eprelErrors = validateEprelData(eprelData, category);

      if (eprelErrors.isEmpty()) {
        validProducts.add(mapEprelToProduct(record, eprelData, orgId, productFileId));
        log.debug("[PRODUCT_UPLOAD] - EPREL product validated: {}", record.get("Codice EPREL"));
      } else {
        errors.addAll(eprelErrors);
      }

    } catch (Exception e) {
      log.error("[PRODUCT_UPLOAD] - EPREL error for code {}: {}", record.get("Codice EPREL"), e.getMessage());
      errors.add("EPREL call error: " + e.getMessage());
    }

    return errors;
  }

  private List<String> buildErrorRow(CSVRecord record, Set<String> headers, List<String> errors) {
    List<String> row = new ArrayList<>();
    headers.forEach(h -> row.add(record.get(h)));
    row.add(String.join(", ", errors));
    return row;
  }

  private void setFinalProductFileStatus(String fileName, String status) {
    Optional<ProductFile> productFile = productFileRepository.findById(fileName.replace(".csv", ""));
    if (productFile.isPresent()) {
      productFile.get().setUploadStatus(status);
      productFileRepository.save(productFile.get());
      log.info("[PRODUCT_UPLOAD] - Final status for file {} set to: {}", fileName, status);
    }
  }

  private Product mapCookingHobToProduct(CSVRecord record, String orgId, String productFileId) {
    return Product.builder()
      .productFileId(productFileId)
      .organizationId(orgId)
      .registrationDate(LocalDateTime.now())
      .status("APPROVED")
      .productCode(record.get("Codice Prodotto"))
      .gtinCode(record.get("Codice GTIN/EAN"))
      .category("COOKINGHOBS")
      .countryOfProduction(record.get("Paese di Produzione"))
      .brand(record.get("Marca"))
      .model(record.get("Modello"))
      .build();
  }

  private Product mapEprelToProduct(CSVRecord record, EprelProductDTO eprelData, String orgId, String productFileId) {
    return Product.builder()
      .productFileId(productFileId)
      .organizationId(orgId)
      .registrationDate(LocalDateTime.now())
      .status("APPROVED")
      .productCode(record.get("Codice Prodotto"))
      .gtinCode(record.get("Codice GTIN/EAN"))
      .eprelCode(record.get("Codice EPREL"))
      .category(eprelData.getProductGroup())
      .countryOfProduction(record.get("Paese di Produzione"))
      .brand(eprelData.getSupplierOrTrademarker())
      .model(eprelData.getModelIdentifier())
      .energyClass(eprelData.getEnergyClass())
      .linkEprel(generateEprelUrl(eprelData.getProductGroup(), record.get("Codice EPREL")))
      .build();
  }

  private String generateEprelUrl(String productGroup, String eprelCode) {
    return String.format("https://eprel.ec.europa.eu/screen/product/%s/%s", productGroup, eprelCode);
  }

  private List<String> validateEprelData(EprelProductDTO eprelData, String expectedCategory) {
    List<String> errors = new ArrayList<>();

    if (eprelData == null) {
      errors.add("Product not found in EPREL");
      return errors;
    }

    if (!"VERIFIED".equalsIgnoreCase(eprelData.getOrgVerificationStatus())) {
      errors.add("orgVerificationStatus is not VERIFIED");
    }

    if (!"VERIFIED".equalsIgnoreCase(eprelData.getTrademarkVerificationStatus())) {
      errors.add("trademarkVerificationStatus is not VERIFIED");
    }

    if (Boolean.TRUE.equals(eprelData.getBlocked())) {
      errors.add("Product is blocked");
    }

    if (!"PUBLISHED".equalsIgnoreCase(eprelData.getStatus())) {
      errors.add("Status is not PUBLISHED");
    }

    if (eprelData.getProductGroup() == null ||
      !eprelData.getProductGroup().toLowerCase().startsWith(expectedCategory.toLowerCase())) {
      errors.add("Product group from EPREL is not compatible with expected category");
    }

    if (!isEnergyClassValid(eprelData.getEnergyClass(), expectedCategory)) {
      String requiredClass = ENERGY_CLASS_REQUIREMENTS.get(expectedCategory.toUpperCase());
      errors.add(String.format("Energy class %s is not compliant. Minimum required: %s",
        eprelData.getEnergyClass(), requiredClass));
    }

    return errors;
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
}
