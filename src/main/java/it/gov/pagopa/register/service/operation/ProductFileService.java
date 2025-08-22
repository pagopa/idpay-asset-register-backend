package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.dto.operation.*;
import it.gov.pagopa.register.enums.UploadCsvStatus;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.mapper.operation.ProductFileMapper;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import it.gov.pagopa.register.service.validator.ProductFileValidatorService;
import it.gov.pagopa.register.utils.CsvUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.constants.LogConstants.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProductFileService {


  private final ProductFileRepository productFileRepository;
  private final ProductRepository productRepository;
  private final FileStorageClient fileStorageClient;
  private final ProductFileValidatorService productFileValidator;

  public ProductFileResponseDTO getFilesByPage(String organizationId, Pageable pageable) {
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

    String filePath = resolveReportPath(productFile);

    ByteArrayOutputStream result = fileStorageClient.download(filePath);

    if (result == null) {
      log.error("[DOWNLOAD_REPORT] - Report not found on Azure for path: {}", filePath);
      throw new ReportNotFoundException("Report not found on Azure for path: " + filePath);
    }

    log.info("[DOWNLOAD_REPORT] - Report downloaded successfully for file: {}", productFile.getFileName());

    return FileReportDTO.builder()
      .data(result.toByteArray())
      .filename(FilenameUtils.getBaseName(productFile.getFileName()) + "_errors.csv")
      .build();
  }

  private String resolveReportPath(ProductFile productFile) {
    String id = productFile.getId();
    String status = productFile.getUploadStatus();

    if (UploadCsvStatus.PARTIAL.name().equals(status)) {
      return REPORT_PARTIAL_ERROR + id + CSV;
    }

    if (UploadCsvStatus.FORMAL_ERROR.name().equals(status)) {
      return REPORT_FORMAL_ERROR + id + CSV;
    }

    log.error("[DOWNLOAD_REPORT] - Report not available for file: {}", productFile.getFileName());
    throw new ReportNotFoundException("Report not available for file: " + productFile.getFileName());
  }

  public ProductFileResult uploadFile(MultipartFile file, String category, String organizationId,
                                      String userId, String userEmail, String organizationName) {
    try {
      ProductFileResult result = validateFile(file, category, organizationId, userId, userEmail, organizationName);

      if (result.isKo()) {
        return result;
      }

      ProductFile productFile = saveProductFile(
        category, organizationId, userId, userEmail,
        file.getOriginalFilename(), result.getRecords(), organizationName
      );

      String path = String.format("CSV/%s/%s/%s/%s.csv", organizationId, organizationName, category, productFile.getId());
      fileStorageClient.upload(file.getInputStream(), path, file.getContentType());

      log.info(FILE_PROCESSED_LOG, file.getOriginalFilename());
      return ProductFileResult.ok();

    } catch (Exception e) {
      log.error(GENERIC_ERROR_LOG, file.getOriginalFilename(), e);
      return ProductFileResult.ko("GENERIC_ERROR");
    }
  }

  public ProductFileResult validateFile(MultipartFile file, String category, String organizationId,
                                        String userId, String userEmail, String organizationName) {
    if (productFileRepository.existsByOrganizationIdAndUploadStatusIn(organizationId, BLOCKING_STATUSES)) {
      log.warn("[PROCESS_FILE] - Existing file in UPLOADED or IN_PROCESS state for org: {}", organizationId);
      return ProductFileResult.ko(AssetRegisterConstants.UploadKeyConstant.UPLOAD_ALREADY_IN_PROGRESS);
    }

    try {
      String originalFileName = file.getOriginalFilename();
      log.info("[PROCESS_FILE] - Processing file: {} for organizationId: {}", originalFileName, organizationId);

      ValidationResultDTO validation = productFileValidator.validateFile(file, category);
      if (validation.isKo()) {
        log.warn(VALIDATION_FAILED_LOG, originalFileName);
        return ProductFileResult.ko(validation.getErrorKey());
      }

      ValidationResultDTO recordValidation = productFileValidator.validateRecords(
        validation.getRecords(), validation.getHeaders(), category);

      if (recordValidation.isKo()) {
        log.warn(VALIDATION_FAILED_LOG, originalFileName);

        ProductFile productFile = saveProductFile(category, organizationId, userId, userEmail,
          originalFileName, validation.getRecords(), organizationName);

        uploadFormalErrorFile(file, recordValidation, validation.getHeaders(), productFile);

        log.warn("[PROCESS_FILE] - File processed with formal errors: {}", originalFileName);
        return ProductFileResult.ko(AssetRegisterConstants.UploadKeyConstant.REPORT_FORMAL_FILE_ERROR_KEY, productFile.getId());
      }

      return ProductFileResult.ok(validation.getRecords());

    } catch (Exception e) {
      log.error(GENERIC_ERROR_LOG, file.getOriginalFilename(), e);
      return ProductFileResult.ko("GENERIC_ERROR");
    }
  }

  private void uploadFormalErrorFile(MultipartFile file, ValidationResultDTO validationRecords,
                                     List<String> headers, ProductFile productFile) throws IOException {

    Path tempFilePath = createTempFilePath();

    CsvUtils.writeCsvWithErrors(
      validationRecords.getInvalidRecords(),
      headers,
      validationRecords.getErrorMessages(),
      tempFilePath
    );

    String destination = REPORT_FORMAL_ERROR + productFile.getId() + CSV;
    fileStorageClient.upload(Files.newInputStream(tempFilePath), destination, file.getContentType());

    Files.deleteIfExists(tempFilePath);
  }

  @SuppressWarnings("java:S5443") //The system used will be Linux so never create a file without specified permissions
  private Path createTempFilePath() throws IOException {
    if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
      Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
      FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
      return Files.createTempFile("errors-", ".csv", attr);
    }
    return Files.createTempFile("errors-", ".csv");
  }

  private ProductFile saveProductFile(String category, String organizationId, String userId, String userEmail,
                                      String originalFileName, List<CSVRecord> records, String organizationName) {

    return productFileRepository.save(ProductFile.builder()
      .fileName(originalFileName)
      .uploadStatus(UploadCsvStatus.FORMAL_ERROR.name())
      .category(category)
      .findedProductsNumber(records.size())
      .addedProductNumber(NumberUtils.INTEGER_ZERO)
      .userId(userId)
      .organizationId(organizationId)
      .dateUpload(LocalDateTime.now())
      .userEmail(userEmail)
      .organizationName(organizationName)
      .build());
  }

  public List<ProductBatchDTO> retrieveDistinctProductFileIdsBasedOnRole(String organizationId, String organizationSelected, String role) {
    log.info("[GET_PRODUCT_FILES] - Fetching product files for organizationId: {}", organizationId);
     List<Product> productFiles = productRepository
      .retrieveDistinctProductFileIdsBasedOnRole(organizationId, organizationSelected, role);

    log.info("[GET_PRODUCT_FILES] - Fetched {} product files for organizationId: {}", productFiles.size(), organizationId);
    return productFiles.stream()
      .map(ProductFileMapper::toBatchDTO)
      .toList();

  }

}
