package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.dto.operation.FileReportDTO;
import it.gov.pagopa.register.dto.operation.ProductBatchDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResult;
import it.gov.pagopa.register.service.operation.ProductFileService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static it.gov.pagopa.register.constants.ValidationConstants.OBJECT_ID_PATTERN;
import static it.gov.pagopa.register.constants.ValidationConstants.UUID_V4_PATTERN;

@Validated
@RestController
@RequestMapping("/idpay/register")
public class ProductFileController {

  private final ProductFileService productFileService;

  public ProductFileController(ProductFileService productFileService) {
    this.productFileService = productFileService;
  }

  @GetMapping("/product-files")
  public ResponseEntity<ProductFileResponseDTO> getProductFileList(
    @RequestHeader("x-organization-id")
    @Pattern(regexp = UUID_V4_PATTERN)
    String organizationId,

    @PageableDefault(size = 20, sort = "dateUpload", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok().body(productFileService.getFilesByPage(organizationId, pageable));
  }


  @PostMapping(value = "/product-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ProductFileResult> uploadProductFile(

    @RequestHeader("x-organization-id")
    @Pattern(regexp = UUID_V4_PATTERN)
    String organizationId,
    @RequestHeader("x-organization-name") String organizationName,
    @RequestHeader("x-user-id")
    @Pattern(regexp = UUID_V4_PATTERN)
    String userId,

    @RequestHeader("x-user-email")
    @Email
    String userEmail,
    @RequestParam("category") String category,
    @RequestPart("csv") MultipartFile csv
  ) {
    return ResponseEntity.ok(
      productFileService.uploadFile(csv, category, organizationId, userId, userEmail, organizationName)
    );
  }


  @PostMapping(value = "/product-files/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ProductFileResult> verifyProductFile(
    @RequestHeader("x-organization-id")
    @Pattern(regexp = UUID_V4_PATTERN)
    String organizationId,
    @RequestHeader("x-organization-name") String organizationName,
    @RequestHeader("x-user-id")
    @Pattern(regexp = UUID_V4_PATTERN)
    String userId,
    @RequestHeader("x-user-email")
    @Email
    String userEmail,

    @RequestParam(value = "category")
    String category,
    @RequestPart("csv") MultipartFile csv
  ) {
    return ResponseEntity.ok(
      productFileService.validateFile(csv, category, organizationId, userId, userEmail, organizationName)
    );
  }


  @GetMapping("/product-files/{productFileId}/report")
  public ResponseEntity<byte[]> downloadProductFileReport(
    @RequestHeader("x-organization-id")
    @Pattern(regexp = UUID_V4_PATTERN)
    String organizationId,
    @PathVariable String productFileId
  ) {
    FileReportDTO file = productFileService.downloadReport(productFileId, organizationId);
    return ResponseEntity.ok()
      .header("Content-Disposition", "attachment; filename=" + file.getFilename())
      .contentType(MediaType.APPLICATION_JSON)
      .body(file.getData());
  }

  @GetMapping("/product-files/batch-list")
  public ResponseEntity<List<ProductBatchDTO>> getFilteredProductFiles(
    @RequestHeader("x-organization-id")
    @Pattern(regexp = UUID_V4_PATTERN)
    String organizationId
    @RequestHeader(value = "x-organization-selected", required = false) String organizationSelected,
    @RequestHeader("x-organization-role") String role
  ) {
    List<ProductBatchDTO> products = productFileService.retrieveDistinctProductFileIdsBasedOnRole(
      organizationId, organizationSelected, role
    );
    return ResponseEntity.ok(products);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ProductFileResult> handleMaxSizeException(MaxUploadSizeExceededException ex) {
    return ResponseEntity.ok(
      ProductFileResult.ko(AssetRegisterConstants.UploadKeyConstant.MAX_SIZE_FILE_ERROR_KEY)
    );
  }
}
