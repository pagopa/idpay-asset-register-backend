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
import static it.gov.pagopa.register.enums.UploadCsvStatus.FORMAL_ERROR;
import static it.gov.pagopa.register.enums.UploadCsvStatus.PARTIAL;

@Slf4j
@Service
public class ProductFileService {


  public static final List<String> BLOCKING_STATUSES = List.of(
    UploadCsvStatus.IN_PROCESS.name(),
    UploadCsvStatus.UPLOADED.name()
  );
  public static final String PROCESS_FILE_VALIDATION_FAILED_FOR_FILE = "[PROCESS_FILE] - Validation failed for file: {}";
  private final ProductFileRepository productFileRepository;
  private final ProductRepository productRepository;

  private final FileStorageClient fileStorageClient;

  private final ProductFileValidatorService productFileValidator;

  public ProductFileService(ProductFileRepository productFileRepository,
                            ProductRepository productRepository, FileStorageClient fileStorageClient,
                            ProductFileValidatorService productFileValidator) {
    this.productFileRepository = productFileRepository;
    this.productRepository = productRepository;
    this.productFileValidator = productFileValidator;
    this.fileStorageClient = fileStorageClient;
  }

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

    String filePath;

    if (PARTIAL.name().equals(productFile.getUploadStatus())) {
      filePath = REPORT_PARTIAL_ERROR + productFile.getId() + CSV;
    } else if (FORMAL_ERROR.name().equals(productFile.getUploadStatus())) {
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



  public ProductFileResult uploadFile(MultipartFile file, String category, String organizationId, String userId, String userEmail, String organizationName){
    try {
      ProductFileResult result = validateFile(file, category, organizationId, userId, userEmail,organizationName);
      if("KO".equals(result.getStatus())){
        return result;
      }
      String originalFileName = file.getOriginalFilename();
      ProductFile productFile = saveProductFile(category, organizationId, userId, userEmail, originalFileName, result.getRecords(),organizationName);
      fileStorageClient.upload(file.getInputStream(), "CSV/" + organizationId + "/" + organizationName + "/" + category + "/" + productFile.getId() + ".csv", file.getContentType());
      log.info("[PROCESS_FILE] - File processed and uploaded successfully: {}", originalFileName);
      return ProductFileResult.ok();
    } catch (Exception e) {
      log.error("[PROCESS_FILE] - Generic Error processing file: {}", file.getOriginalFilename(), e);
      return ProductFileResult.ko("GENERIC_ERROR");
    }
  }



  public ProductFileResult validateFile(MultipartFile file, String category, String organizationId, String userId, String userEmail, String organizationName) {
    boolean alreadyBlocked = productFileRepository.existsByOrganizationIdAndUploadStatusIn(
      organizationId, BLOCKING_STATUSES
    );
    if (alreadyBlocked) {
      log.warn("[PROCESS_FILE] - Existing file in UPLOADED or IN_PROCESS state for org: {}", organizationId);
      return ProductFileResult.ko(AssetRegisterConstants.UploadKeyConstant.UPLOAD_ALREADY_IN_PROGRESS);
    }
    try {
      String originalFileName = file.getOriginalFilename();
      log.info("[PROCESS_FILE] - Processing file: {} for organizationId: {}", originalFileName, organizationId);
      ValidationResultDTO validation = productFileValidator.validateFile(file, category);
      if ("KO".equals(validation.getStatus())) {
        log.warn(PROCESS_FILE_VALIDATION_FAILED_FOR_FILE, originalFileName);
        return ProductFileResult.ko(validation.getErrorKey());
      }
      ValidationResultDTO validationRecords = productFileValidator.validateRecords(validation.getRecords(), validation.getHeaders(), category);
      if ("KO".equals(validationRecords.getStatus())) {
        log.warn(PROCESS_FILE_VALIDATION_FAILED_FOR_FILE, originalFileName);
        ProductFile productFile = saveProductFile(category, organizationId, userId, userEmail, originalFileName, validation.getRecords(),organizationName);
        uploadFormalErrorFile(file, validationRecords, validation.getHeaders(), productFile);
        log.warn("[PROCESS_FILE] - File processed with formal errors: {}", originalFileName);
        return ProductFileResult.ko(AssetRegisterConstants.UploadKeyConstant.REPORT_FORMAL_FILE_ERROR_KEY, productFile.getId());
      }
      return ProductFileResult.ok(validation.getRecords());
    } catch (Exception e) {
      log.error("[PROCESS_FILE] - Generic Error processing file: {}", file.getOriginalFilename(), e);
      return ProductFileResult.ko("GENERIC_ERROR");
    }
  }

  @SuppressWarnings("java:S5443") // The system used will be Linux so never create a file without specified permissions
  private void uploadFormalErrorFile(MultipartFile file, ValidationResultDTO validationRecords, List<String> headers, ProductFile productFile) throws IOException {
    Path tempFilePath;
    if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
      Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
      FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
      tempFilePath = Files.createTempFile("errors-", ".csv", attr);
    } else {
      tempFilePath = Files.createTempFile("errors-", ".csv");
    }
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



  private ProductFile saveProductFile(String category, String organizationId, String userId, String userEmail, String originalFileName, List<CSVRecord> records, String organizationName) {

    return productFileRepository.save(ProductFile.builder()
      .fileName(originalFileName)
      .uploadStatus(FORMAL_ERROR.name())
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
