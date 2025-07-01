package it.gov.pagopa.register.service.operation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.kafka.BaseKafkaConsumer;
import it.gov.pagopa.register.connector.eprel.EprelConnector;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.dto.operation.EprelProductDTO;
import it.gov.pagopa.register.dto.operation.StorageEventDTO;
import it.gov.pagopa.register.dto.operation.ProductFileDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.mapper.operation.ProductFileMapper;
import it.gov.pagopa.register.model.role.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
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
import java.util.regex.Pattern;

import static it.gov.pagopa.register.constants.enums.UploadCsvStatus.EPREL_ERROR;
import static it.gov.pagopa.register.constants.enums.UploadCsvStatus.LOADED;

@Slf4j
@Service
public class ProductFileService extends BaseKafkaConsumer<List<StorageEventDTO>> {

  private static final Pattern SUBJECT_PATTERN = Pattern.compile(".*blobs/CSV/(.*?)-(.*?)-(.*?)-(.*?\\.csv)$");

  private static final Map<String, String> ENERGY_CLASS_REQUIREMENTS = Map.of(
    "WASHINGMACHINES", "A",
    "WASHERDRIERS", "A",
    "OVENS", "A",
    "RANGEHOODS", "B",
    "DISHWASHERS", "C",
    "TUMBLEDRIERS", "C",
    "REFRIGERATINGAPPLIANCES", "D"
  );

  private static final List<String> ENERGY_CLASS_ORDER = Arrays.asList(
    "A+++", "A++", "A+", "A", "B", "C", "D", "E", "F", "G"
  );

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
      String subject = event.getSubject();
      String url = event.getData().getUrl();

      Matcher matcher = SUBJECT_PATTERN.matcher(subject);
      if (!matcher.find() || matcher.groupCount() < 4) {
        log.warn("[PRODUCT_UPLOAD] - Invalid subject format: {}", subject);
        continue;
      }

      String orgId = matcher.group(1);
      String category = matcher.group(2);
      String userId = matcher.group(3);
      String fileName = matcher.group(4);

      log.info("[PRODUCT_UPLOAD] - Processing file: {} for orgId={}, category={}, userId={}", fileName, orgId, category, userId);

      try (ByteArrayOutputStream inputStream = fileStorageClient.download(url)) {
        processCsvFromStorage(inputStream, fileName, category, orgId, userId);
      } catch (Exception e) {
        log.error("[PRODUCT_UPLOAD] - Error processing file {}: {}", fileName, e.getMessage(), e);
        setFinalProductFileStatus(fileName, orgId, String.valueOf(EPREL_ERROR));
      }
    }
  }

  public void processCsvFromStorage(ByteArrayOutputStream byteArrayOutputStream,
                                    String fileName,
                                    String category,
                                    String orgId,
                                    String userId) {

    List<Product> validProducts = new ArrayList<>();
    Map<Integer, List<String>> errorRows = new LinkedHashMap<>();
    boolean isCookingHob = "COOKINGHOBS".equalsIgnoreCase(category);
    String productFileId = getExistingProductFileId(fileName, orgId);

    try (InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
         CSVParser csvParser = new CSVParser(reader, CSVFormat.Builder.create()
           .setHeader()
           .setTrim(true)
           .setDelimiter(';')
           .build())) {

      List<CSVRecord> records = csvParser.getRecords();
      Set<String> headers = new LinkedHashSet<>(csvParser.getHeaderMap().keySet());
      log.info("[PRODUCT_UPLOAD] - Header CSV validi: {}", headers);

      for (int i = 0; i < records.size(); i++) {
        CSVRecord record = records.get(i);
        List<String> errors = processCsvRecord(record, orgId, category, productFileId, validProducts, isCookingHob);
        if (!errors.isEmpty()) {
          errorRows.put(i + 1, buildErrorRow(record, headers, errors));
        }
      }

      if (!validProducts.isEmpty()) {
        productRepository.saveAll(validProducts);
        log.info("[PRODUCT_UPLOAD] - Salvati {} prodotti validi per file {}", validProducts.size(), fileName);
      }

      if (!errorRows.isEmpty()) {
        generateErrorReport(fileName, errorRows, headers, orgId, category, userId);
        setFinalProductFileStatus(fileName, orgId, String.valueOf(EPREL_ERROR));
        log.info("[PRODUCT_UPLOAD] - File {} processato con {} errori EPREL", fileName, errorRows.size());
      } else {
        setFinalProductFileStatus(fileName, orgId, String.valueOf(LOADED));
        log.info("[PRODUCT_UPLOAD] - File {} caricato completamente senza errori", fileName);
      }

    } catch (IOException e) {
      log.error("[PRODUCT_UPLOAD] - Errore lettura CSV {}: {}", fileName, e.getMessage());
      setFinalProductFileStatus(fileName, orgId, String.valueOf(EPREL_ERROR));
    }
  }
  private List<String> processCsvRecord(CSVRecord record, String orgId, String category, String productFileId,
                                        List<Product> validProducts, boolean isCookingHob) {
    List<String> errors = new ArrayList<>();

    if (isCookingHob) {
      validProducts.add(mapCookingHobToProduct(record, orgId, productFileId));
      log.debug("[PRODUCT_UPLOAD] - Piano cottura aggiunto: {}", record.get("Codice Prodotto"));
      return errors;
    }

    try {
      EprelProductDTO eprelData = eprelConnector.callEprel(record.get("Codice EPREL"));
      List<String> eprelErrors = validateEprelData(eprelData, category);

      if (eprelErrors.isEmpty()) {
        validProducts.add(mapEprelToProduct(record, eprelData, orgId, productFileId));
        log.debug("[PRODUCT_UPLOAD] - Prodotto EPREL validato: {}", record.get("Codice EPREL"));
      } else {
        errors.addAll(eprelErrors);
      }

    } catch (Exception e) {
      log.error("[PRODUCT_UPLOAD] - Errore EPREL per codice {}: {}", record.get("Codice EPREL"), e.getMessage());
      errors.add("Errore chiamata EPREL: " + e.getMessage());
    }

    return errors;
  }

  private List<String> buildErrorRow(CSVRecord record, Set<String> headers, List<String> errors) {
    List<String> row = new ArrayList<>();
    headers.forEach(h -> row.add(record.get(h)));
    row.add(String.join(", ", errors));
    return row;
  }


  private String getExistingProductFileId(String fileName, String orgId) {
    Optional<ProductFile> productFile = productFileRepository.findByOrganizationIdAndFileName(orgId, fileName);
    if (productFile.isPresent()) {
      return productFile.get().getProductFileId();
    } else {
      log.error("[PRODUCT_UPLOAD] - ProductFile non trovato per fileName={}, orgId={}", fileName, orgId);
      throw new IllegalStateException("ProductFile non trovato per il file: " + fileName);
    }
  }

  private void setFinalProductFileStatus(String fileName, String orgId, String status) {
    Optional<ProductFile> productFile = productFileRepository.findByOrganizationIdAndFileName(orgId, fileName);
    if (productFile.isPresent()) {
      productFile.get().setUploadStatus(status);
      productFileRepository.save(productFile.get());
      log.info("[PRODUCT_UPLOAD] - Stato finale file {} impostato a: {}", fileName, status);
    }
  }

  private Product mapCookingHobToProduct(CSVRecord record, String orgId, String productFileId) {
    return Product.builder()
      .productFileId(productFileId)
      .organizationId(orgId)
      .registrationDate(LocalDateTime.now())
      .status("PENDING_VALIDATION")
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
      .status("PENDING_VALIDATION")
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
      errors.add("Prodotto non trovato su EPREL");
      return errors;
    }

    if (!"VERIFIED".equalsIgnoreCase(eprelData.getOrgVerificationStatus())) {
      errors.add("orgVerificationStatus non VERIFIED");
    }

    if (!"VERIFIED".equalsIgnoreCase(eprelData.getTrademarkVerificationStatus())) {
      errors.add("trademarkVerificationStatus non VERIFIED");
    }

    if (Boolean.TRUE.equals(eprelData.getBlocked())) {
      errors.add("Prodotto bloccato");
    }

    if (!"PUBLISHED".equalsIgnoreCase(eprelData.getStatus())) {
      errors.add("status non PUBLISHED");
    }

    if (eprelData.getProductGroup() == null ||
      !eprelData.getProductGroup().toLowerCase().startsWith(expectedCategory.toLowerCase())) {
      errors.add("Categoria non compatibile con productGroup EPREL");
    }

    if (!isEnergyClassValid(eprelData.getEnergyClass(), expectedCategory)) {
      String requiredClass = ENERGY_CLASS_REQUIREMENTS.get(expectedCategory.toUpperCase());
      errors.add(String.format("Classe energetica %s non conforme. Richiesta minimo: %s",
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
      log.warn("[PRODUCT_UPLOAD] - Categoria non riconosciuta per validazione classe energetica: {}", category);
      return false;
    }

    int requiredIndex = ENERGY_CLASS_ORDER.indexOf(requiredMinClass);
    int productIndex = ENERGY_CLASS_ORDER.indexOf(energyClass.toUpperCase());

    if (requiredIndex == -1 || productIndex == -1) {
      log.warn("[PRODUCT_UPLOAD] - Classe energetica non riconosciuta. Richiesta: {}, Prodotto: {}",
        requiredMinClass, energyClass);
      return false;
    }

    return productIndex <= requiredIndex;
  }

  private void generateErrorReport(String fileName, Map<Integer, List<String>> errorRows,
                                   Set<String> headers, String orgId, String category, String userId) {
    try {
      String reportFileName = fileName.replace(".csv", "_errors.csv");
      ByteArrayOutputStream reportOutputStream = new ByteArrayOutputStream();

      try (OutputStreamWriter writer = new OutputStreamWriter(reportOutputStream, StandardCharsets.UTF_8);
           CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withDelimiter(';'))) {

        List<String> reportHeaders = new ArrayList<>(headers);
        reportHeaders.add("Errori");
        csvPrinter.printRecord(reportHeaders);

        for (List<String> errorRow : errorRows.values()) {
          csvPrinter.printRecord(errorRow);
        }
      }

      String reportPath = String.format("CSV/%s-%s-%s-%s", orgId, category, userId, reportFileName);
      InputStream reportInputStream = new ByteArrayInputStream(reportOutputStream.toByteArray());
      fileStorageClient.upload(reportInputStream, reportPath, "text/csv");

      log.info("[PRODUCT_UPLOAD] - Report errori generato: {}", reportPath);

    } catch (IOException e) {
      log.error("[PRODUCT_UPLOAD] - Errore generazione report per file {}: {}", fileName, e.getMessage(), e);
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
