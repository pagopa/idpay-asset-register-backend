package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.FileReportDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.dto.operation.ProductFileResult;
import it.gov.pagopa.register.service.operation.ProductFileService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/idpay/register")
public class ProductFileController {

  private final ProductFileService productFileService;

  public ProductFileController(ProductFileService productFileService) {
    this.productFileService = productFileService;
  }

  @GetMapping("/product-files")
  public ResponseEntity<ProductFileResponseDTO> downloadProductFileList(
    @RequestHeader("x-organization-id") String organizationId,
    @PageableDefault(size = 10, sort = "dateUpload", direction = Sort.Direction.DESC) Pageable pageable) {

    return ResponseEntity.ok().body(productFileService.getFilesByPage(organizationId, pageable));
  }

  @PostMapping(value = "/product-files", consumes = "multipart/form-data")
  public ResponseEntity<ProductFileResult> uploadProductFile(@RequestHeader("x-organization-id") String organizationId, @RequestHeader("x-user-id") String userId,
                                                             @RequestParam(value = "category") String category, @RequestPart("csv") MultipartFile csv) {
    ProductFileResult productFileResult = productFileService.processFile(csv, category, organizationId, userId);
    return ResponseEntity.ok().body(productFileResult);
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

}
