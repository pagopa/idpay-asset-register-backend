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

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.constants.enums.UploadCsvStatus.FORMAL_ERROR;
import static it.gov.pagopa.register.constants.enums.UploadCsvStatus.UPLOADED;

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

    if (AssetRegisterConstants.EPREL_ERROR.equals(productFile.getUploadStatus())) {
      filePath = REPORT_EPREL_ERROR + productFile.getId() + CSV;
    } else if (AssetRegisterConstants.FORMAL_ERROR.equals(productFile.getUploadStatus())) {
      filePath = REPORT_FORMAL_ERROR + productFile.getId() + CSV;
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
        .build());

      // Upload on Azure
      fileStorageClient.upload(file.getInputStream(), "CSV/"+organizationId+"/"+category+"/"+productFile.getId()+".csv", file.getContentType());

      return ProductFileResult.ok();

    } catch (Exception e) {
      log.error("[UPLOAD_PRODUCT_FILE] - Generic Error ", e);
      return ProductFileResult.ko("GENERIC_ERROR");
    }
  }
}
