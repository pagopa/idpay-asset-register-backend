package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.service.operation.ProductFileService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/idpay/register")
public class ProductFileController {
  private final ProductFileService productFileService;

  public ProductFileController(ProductFileService productFileService) {
    this.productFileService = productFileService;
  }

  @GetMapping("/product-files")
  public ProductFileResponseDTO downloadListUpload(
    @RequestHeader("x-organization-id") String organizationId,
    @PageableDefault(size = 10, sort = "dateUpload", direction = Sort.Direction.DESC) Pageable pageable) {

    return productFileService.downloadFilesByPage(organizationId, pageable);
  }
}
