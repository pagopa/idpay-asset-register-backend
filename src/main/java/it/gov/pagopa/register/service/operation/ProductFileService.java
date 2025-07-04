package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.config.ProductFileValidationConfig;
import it.gov.pagopa.register.connector.storage.FileStorageClient;
import it.gov.pagopa.register.constants.AssetRegisterConstant;
import it.gov.pagopa.register.constants.enums.UploadCsvStatus;
import it.gov.pagopa.register.dto.operation.*;
import it.gov.pagopa.register.exception.operation.ReportNotFoundException;
import it.gov.pagopa.register.mapper.operation.ProductFileMapper;
import it.gov.pagopa.register.model.operation.ProductFile;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.utils.CsvUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;


@Service
@Slf4j
public class ProductFileService {

  private final ProductFileRepository productFileRepository;
  private final FileStorageClient azureBlobClient;
  private final ProductFileValidator productFileValidator;

  @Autowired
  private ProductFileValidationConfig validationConfig;

  public ProductFileService(ProductFileRepository productFileRepository, FileStorageClient azureBlobClient, ProductFileValidator productFileValidator) {
    this.productFileRepository = productFileRepository;
    this.azureBlobClient = azureBlobClient;
    this.productFileValidator = productFileValidator;
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

    ByteArrayOutputStream result = azureBlobClient.download(filePath);

    if (result == null) {
      throw new ReportNotFoundException("Report not found on Azure for path: " + filePath);
    }

    return FileReportDTO.builder().data(result).filename(FilenameUtils.getBaseName(productFile.getFileName()) + "_errors.csv").build();
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
          .uploadStatus(UploadCsvStatus.FORMAL_ERROR.name())
          .category(category)
          .findedProductsNumber(records.size())
          .addedProductNumber(NumberUtils.INTEGER_ZERO)
          .userId(userId)
          .organizationId(organizationId)
          .dateUpload(LocalDateTime.now())
          .build());

        Path tempFilePath = Paths.get("/tmp/", errorFileName);
        String destination = "Report/Formal_Error/" + productFile.getId() + ".csv";
        azureBlobClient.upload(Files.newInputStream(tempFilePath), destination, file.getContentType());

        return ProductFileResult.ko(AssetRegisterConstant.UploadKeyConstant.REPORT_FORMAL_FILE_ERROR_KEY, productFile.getId());
      }

      // Upload on Azure
      azureBlobClient.upload(file.getInputStream(), "/CSV/"+originalFileName, file.getContentType());

      // Log OK
      productFileRepository.save(ProductFile.builder()
        .fileName(originalFileName)
        .uploadStatus(UploadCsvStatus.UPLOADED.name())
        .category(category)
        .findedProductsNumber(records.size())
        .addedProductNumber(NumberUtils.INTEGER_ZERO)
        .userId(userId)
        .organizationId(organizationId)
        .dateUpload(LocalDateTime.now())
        .build());

      return ProductFileResult.ok();

    } catch (Exception e) {
      log.error("[UPLOAD_PRODUCT_FILE] - Generic Error ", e);
      return ProductFileResult.ko("GENERIC_ERROR");
    }
  }

}
