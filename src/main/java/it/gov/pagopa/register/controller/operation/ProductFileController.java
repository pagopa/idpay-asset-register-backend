package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.dto.operation.FileReportDTO;
import it.gov.pagopa.register.dto.operation.ProductBatchDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResult;
import it.gov.pagopa.register.service.operation.ProductFileService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/idpay/register")
public class ProductFileController {

  private final ProductFileService productFileService;

  public ProductFileController(ProductFileService productFileService) {
    this.productFileService = productFileService;
  }

  @GetMapping("/product-files")
  public ResponseEntity<ProductFileResponseDTO> getProductFileList(
    @RequestHeader("x-organization-id") String organizationId,
    @PageableDefault(size = 20, sort = "dateUpload", direction = Sort.Direction.DESC) Pageable pageable) {

    return ResponseEntity.ok().body(productFileService.getFilesByPage(organizationId, pageable));
  }

  @PostMapping(value = "/product-files", consumes = "multipart/form-data")
  public ResponseEntity<ProductFileResult> uploadProductFile(@RequestHeader("x-organization-id") String organizationId,
                                                             @RequestHeader("x-user-id") String userId,
                                                             @RequestHeader("x-user-email") String userEmail,
                                                             @RequestHeader("x-organization-name") String organizationName,
                                                             @RequestParam(value = "category") String category,
                                                             @RequestPart("csv") MultipartFile csv) {

    ProductFileResult productFileResult = productFileService.uploadFile(csv, category, organizationId, userId, userEmail, organizationName);
    return ResponseEntity.ok().body(productFileResult);
  }

  @PostMapping(value = "/product-files/verify", consumes = "multipart/form-data")
  public ResponseEntity<ProductFileResult> verifyProductFile(@RequestHeader("x-organization-id") String organizationId,
                                                             @RequestHeader("x-user-id") String userId,
                                                             @RequestHeader("x-user-email") String userEmail,
                                                             @RequestParam(value = "category") String category,
                                                             @RequestHeader("x-organization-name") String organizationName,
                                                             @RequestPart("csv") MultipartFile csv) {
    ProductFileResult productFileResult = productFileService.validateFile(csv, category, organizationId, userId, userEmail,organizationName);
    return ResponseEntity.ok().body(productFileResult);
  }


  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ProductFileResult> handleMaxSizeException(MaxUploadSizeExceededException ex) {
    return ResponseEntity.ok().body(ProductFileResult.ko(AssetRegisterConstants.UploadKeyConstant.MAX_SIZE_FILE_ERROR_KEY));
  }



  @GetMapping("/product-files/{productFileId}/report")
  public ResponseEntity<byte[]> downloadProductFileReport(
    @RequestHeader("x-organization-id") String organizationId,
    @PathVariable("productFileId") String productFileId) {

    FileReportDTO file = productFileService.downloadReport(productFileId, organizationId);

    return ResponseEntity.ok()
      .header("Content-Disposition", "attachment; filename="+file.getFilename())
      .contentType(MediaType.APPLICATION_JSON)
      .body(file.getData());
  }

  @GetMapping("/product-files/batch-list")
  public ResponseEntity<List<ProductBatchDTO>> getFileteredProductFiles(
    @RequestHeader("x-organization-id") String organizationId,
     @RequestHeader("x-organization-role") String role
  ) {
    return ResponseEntity.ok().body(productFileService.retrieveDistinctProductFileIdsBasedOnRole(organizationId,role));
  }
}
