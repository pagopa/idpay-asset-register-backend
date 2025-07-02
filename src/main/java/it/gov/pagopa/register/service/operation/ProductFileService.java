package it.gov.pagopa.register.service.operation;
import it.gov.pagopa.register.config.ProductFileValidationConfig;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.constants.AssetRegisterConstant;
import it.gov.pagopa.register.dto.operation.ProductFileResult;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.mapper.operation.ProductFileMapper;
import it.gov.pagopa.register.dto.operation.ProductFileDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.utils.CsvUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.*;


@Service
@Slf4j
public class ProductFileService {

  private final ProductFileRepository productFileRepository;
  private final FileStorageClient azureBlobClient;
  private final ProductFileValidator productFileValidator;

  @Autowired
  private ProductFileValidationConfig validationConfig;

  public ProductFileService(ProductFileRepository productFileRepository, FileStorageClient azureBlobClient,ProductFileValidator productFileValidator) {
    this.productFileRepository = productFileRepository;
    this.azureBlobClient = azureBlobClient;
    this.productFileValidator = productFileValidator;
  }

  public ProductFileResponseDTO getFilesByPage(String organizationId, Pageable pageable) {

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

  public ByteArrayOutputStream downloadReport(String id, String organizationId) {
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

    ByteArrayOutputStream result = azureBlobClient.download(filePath);

    if (result == null) {
      throw new ReportNotFoundException("Report non trovato su Azure per path: " + filePath);
    }

    return result;
  }

  public ProductFileResult processFile(MultipartFile file, String category, String idOrg, String idUser) {

    try {
      String originalFileName = file.getOriginalFilename();
      String batchId = UUID.randomUUID().toString();
      Instant now = Instant.now();

      // 0. Validazione preliminare: file vuoto
      if (file == null || file.isEmpty() || file.getSize() == 0) {
        return ProductFileResult.ko("EMPTY_FILE");
      }

      // 0.1. Verifica estensione e tipo mime (grezza ma utile)
      if (!originalFileName.endsWith(".csv")) {
        return ProductFileResult.ko("INVALID_FILE_TYPE");
      }

      // 1. Carica configurazione
      LinkedHashMap<String, ColumnValidationRule> columnDefinitions = validationConfig.getSchemas().getOrDefault(category, validationConfig.getSchemas().get("eprel"));
      if (columnDefinitions == null || columnDefinitions.isEmpty()) {
        return ProductFileResult.ko("UNKNOWN_CATEGORY");
      }

      // 2. Legge header
      List<String> actualHeader = CsvUtils.readHeader(file);
      List<String> expectedHeader = new ArrayList<>(columnDefinitions.keySet());

      if (!actualHeader.equals(expectedHeader)) {
        return ProductFileResult.ko("INVALID_HEADER");
      }

      // 3. Legge record (dopo aver letto l'header)
      List<CSVRecord> records = CsvUtils.readCsvRecords(file);
      int totalRecords = records.size();

      if (totalRecords == 0) {
        return ProductFileResult.ko("NO_DATA");
      }

      if (totalRecords > 100) {
        return ProductFileResult.ko("TOO_MANY_RECORDS");
      }

      // 4. Validazione dei singoli record
      List<CSVRecord> invalidRecords = new ArrayList<>();
      Map<CSVRecord, String> errorMessages = new HashMap<>();

      for (CSVRecord record : records) {
        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, ColumnValidationRule> entry : columnDefinitions.entrySet()) {
          String columnName = entry.getKey();
          ColumnValidationRule def = entry.getValue();
          String value = record.get(columnName);

          if (def.isValid(value)) {
            errors.add(def.getMessage());
          }
        }

        if (!errors.isEmpty()) {
          invalidRecords.add(record);
          errorMessages.put(record, String.join(" | ", errors));
        }
      }

      // 5. Se ci sono errori, salva file scarti
      int invalidCount = invalidRecords.size();
      if (invalidCount > 0) {
        String scartiFilename = "scarti_" + originalFileName;
        CsvUtils.writeCsvWithErrors(invalidRecords, expectedHeader, errorMessages, scartiFilename);

/*        productFileRepository.save(new ProcessingLog(
          batchId, originalFileName, category, totalRecords, invalidCount,
          "FORMAL_ERROR", now
        ));
*/
        return ProductFileResult.ko("FORMAL_ERRORS_FOUND");
      }
/*
      // 6. Upload su Azure
      azureBlobClient.upload(file, batchId);

      // 7. Log OK
      productFileRepository.save(new ProcessingLog(
        batchId, originalFileName, category, totalRecords, 0,
        "LOADED", now
      ));
*/
      return ProductFileResult.ok();

    } catch (Exception e) {
      e.printStackTrace();
      return ProductFileResult.ko("GENERIC_ERROR");
    }
  }

}
