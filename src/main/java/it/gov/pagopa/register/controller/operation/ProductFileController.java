package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.dto.operation.*;
import it.gov.pagopa.register.service.operation.ProductFileService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
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

import static it.gov.pagopa.register.constants.ValidationPatterns.*;

@Validated
@RestController
@RequestMapping("/idpay/register")
@RequiredArgsConstructor
public class ProductFileController {

  private final ProductFileService productFileService;

  @GetMapping("/product-files")
  public ResponseEntity<ProductFileResponseDTO> getProductFileList(
    @RequestHeader("x-organization-id") @Pattern(regexp = UUID_V4_PATTERN) String organizationId,
    @PageableDefault(size = 20, sort = "dateUpload", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok().body(productFileService.getFilesByPage(organizationId, pageable));
  }

  @PostMapping(value = "/product-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ProductFileResult> uploadProductFile(
    @RequestHeader("x-organization-id") @Pattern(regexp = UUID_V4_PATTERN) String organizationId,
    @RequestHeader("x-organization-name") String organizationName,
    @RequestHeader("x-user-id") @Pattern(regexp = UUID_V4_PATTERN) String userId,
    @RequestHeader("x-user-email") @Email String userEmail,
    @RequestParam("category") String category,
    @RequestPart("csv") MultipartFile csv
  ) {
    ProductFileResult result = productFileService.uploadFile(csv, category, organizationId, userId, userEmail, organizationName);
    return ResponseEntity.ok(result);
  }

  @PostMapping(value = "/product-files/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ProductFileResult> verifyProductFile(
    @RequestHeader("x-organization-id") @Pattern(regexp = UUID_V4_PATTERN) String organizationId,
    @RequestHeader("x-organization-name") String organizationName,
    @RequestHeader("x-user-id") @Pattern(regexp = UUID_V4_PATTERN) String userId,
    @RequestHeader("x-user-email") @Email String userEmail,
    @RequestParam("category") String category,
    @RequestPart("csv") MultipartFile csv
  ) {
    ProductFileResult result = productFileService.validateFile(csv, category, organizationId, userId, userEmail, organizationName);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/product-files/{productFileId}/report")
  public ResponseEntity<byte[]> downloadProductFileReport(
    @RequestHeader("x-organization-id") @Pattern(regexp = UUID_V4_PATTERN) String organizationId,
    @PathVariable @Pattern(regexp = OBJECT_ID_PATTERN) String productFileId
  ) {
    FileReportDTO file = productFileService.downloadReport(productFileId, organizationId);
    return ResponseEntity.ok()
      .header("Content-Disposition", "attachment; filename=" + file.getFilename())
      .contentType(MediaType.APPLICATION_JSON)
      .body(file.getData());
  }

  @GetMapping("/product-files/batch-list")
  public ResponseEntity<List<ProductBatchDTO>> getFilteredProductFiles(
    @RequestHeader("x-organization-id") @Pattern(regexp = UUID_V4_PATTERN) String organizationId,
    @RequestHeader(value = "x-organization-selected", required = false) @Pattern(regexp = UUID_V4_PATTERN) String organizationSelected,
    @RequestHeader("x-organization-role") @Pattern(regexp = ROLE_PATTERN) String role
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
