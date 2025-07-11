package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.constants.enums.UploadCsvStatus;
import it.gov.pagopa.register.dto.operation.*;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.mapper.operation.ProductFileMapper;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.service.validator.ProductFileValidatorService;
import it.gov.pagopa.register.utils.CsvUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.constants.enums.UploadCsvStatus.FORMAL_ERROR;
import static it.gov.pagopa.register.constants.enums.UploadCsvStatus.UPLOADED;
import static it.gov.pagopa.register.constants.enums.UploadCsvStatus.PARTIAL;

@Slf4j
@Service
public class ProductFileService {


  private final ProductFileRepository productFileRepository;

  private final FileStorageClient fileStorageClient;

  private final ProductFileValidatorService productFileValidator;

  public ProductFileService(ProductFileRepository productFileRepository,
                            FileStorageClient fileStorageClient,
                            ProductFileValidatorService productFileValidator) {
    this.productFileRepository = productFileRepository;
    this.productFileValidator = productFileValidator;
    this.fileStorageClient = fileStorageClient;
  }

  public ProductFileResponseDTO getFilesByPage(String organizationId, Pageable pageable) {

    if (organizationId == null) {
      log.error("[GET_FILES_BY_PAGE] - OrganizationId must not be null");
      throw new IllegalArgumentException("OrganizationId must not be null");
    }

    log.info("[GET_FILES_BY_PAGE] - Fetching files for organizationId: {}", organizationId);
    Page<ProductFile> filesPage = productFileRepository.findByOrganizationIdAndUploadStatusNot(
      organizationId, UploadCsvStatus.FORMAL_ERROR.name(), pageable);

    Page<ProductFileDTO> filesPageDTO = filesPage.map(ProductFileMapper::toDTO);

    log.info("[GET_FILES_BY_PAGE] - Fetched {} files", filesPageDTO.getTotalElements());
    return ProductFileResponseDTO.builder()
      .content(filesPageDTO.getContent())
      .pageNo(filesPageDTO.getNumber())
      .pageSize(filesPageDTO.getSize())
      .totalElements(filesPageDTO.getTotalElements())
      .totalPages(filesPageDTO.getTotalPages())
      .build();
  }

  public FileReportDTO downloadReport(String id, String organizationId) {
    log.info("[DOWNLOAD_REPORT] - Downloading report for id: {} and organizationId: {}", id, organizationId);
    ProductFile productFile = productFileRepository.findByIdAndOrganizationId(id, organizationId)
      .orElseThrow(() -> {
        log.error("[DOWNLOAD_REPORT] - Report not found with id: {}", id);
        return new ReportNotFoundException("Report not found with id: " + id);
      });

    String filePath;

    if (AssetRegisterConstants.EPREL_ERROR.equals(productFile.getUploadStatus())) {
      filePath = REPORT_EPREL_ERROR + productFile.getId() + CSV;
    } else if (AssetRegisterConstants.FORMAL_ERROR.equals(productFile.getUploadStatus())) {
      filePath = REPORT_FORMAL_ERROR + productFile.getId() + CSV;
    } else {
      log.error("[DOWNLOAD_REPORT] - Report not available for file: {}", productFile.getFileName());
      throw new ReportNotFoundException("Report not available for file: " + productFile.getFileName());
    }

    ByteArrayOutputStream result = fileStorageClient.download(filePath);

    if (result == null) {
      log.error("[DOWNLOAD_REPORT] - Report not found on Azure for path: {}", filePath);
      throw new ReportNotFoundException("Report not found on Azure for path: " + filePath);
    }

    log.info("[DOWNLOAD_REPORT] - Report downloaded successfully for file: {}", productFile.getFileName());
    return FileReportDTO.builder().data(result.toByteArray()).filename(FilenameUtils.getBaseName(productFile.getFileName()) + "_errors.csv").build();
  }

  public ProductFileResult processFile(MultipartFile file, String category, String organizationId, String userId, String userEmail) {

    try {
      String originalFileName = file.getOriginalFilename();
      log.info("[PROCESS_FILE] - Processing file: {} for organizationId: {}", originalFileName, organizationId);
      List<String> headers = CsvUtils.readHeader(file);
      List<CSVRecord> records = CsvUtils.readCsvRecords(file);

      //TODO check if for the specified organization there are some file uploaded or in progress and stop the upload of new file

      ValidationResultDTO validation = productFileValidator.validateFile(file, category, headers, records.size());
      if ("KO".equals(validation.getStatus())) {
        log.warn("[PROCESS_FILE] - Validation failed for file: {}", originalFileName);
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
          .userEmail(userEmail)
          .build());

        Path tempFilePath = Paths.get("/tmp/", errorFileName);
        String destination = "Report/Formal_Error/" + productFile.getId() + ".csv";
        fileStorageClient.upload(Files.newInputStream(tempFilePath), destination, file.getContentType());

        log.warn("[PROCESS_FILE] - File processed with formal errors: {}", originalFileName);
        return ProductFileResult.ko(AssetRegisterConstants.UploadKeyConstant.REPORT_FORMAL_FILE_ERROR_KEY, productFile.getId());
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
        .userEmail(userEmail)
        .build());

      // Upload on Azure
      fileStorageClient.upload(file.getInputStream(), "CSV/" + organizationId + "/" + category + "/" + productFile.getId() + ".csv", file.getContentType());

      log.info("[PROCESS_FILE] - File processed and uploaded successfully: {}", originalFileName);
      return ProductFileResult.ok();

    } catch (Exception e) {
      log.error("[PROCESS_FILE] - Generic Error processing file: {}", file.getOriginalFilename(), e);
      return ProductFileResult.ko("GENERIC_ERROR");
    }
  }

  public List<ProductBatchDTO> getProductFilesByOrganizationId(String organizationId) {
    if (Objects.isNull(organizationId) || organizationId.isEmpty()) {
      log.error("[GET_PRODUCT_FILES] - Organization Id is null or empty");
      throw new ReportNotFoundException("Organization Id is null or empty");
    }

    log.info("[GET_PRODUCT_FILES] - Fetching product files for organizationId: {}", organizationId);
    List<String> excludedStatuses = List.of(
      FORMAL_ERROR.name(),
      PARTIAL.name()
    );

    List<ProductBatchDTO> productFiles = productFileRepository
      .findByOrganizationIdAndUploadStatusNotIn(organizationId, excludedStatuses)
      .orElse(List.of())
      .stream()
      .map(ProductFileMapper::toBatchDTO)
      .toList();

    log.info("[GET_PRODUCT_FILES] - Fetched {} product files for organizationId: {}", productFiles.size(), organizationId);
    return productFiles;
  }

}
