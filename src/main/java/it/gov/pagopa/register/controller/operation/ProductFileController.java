package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.service.operation.ProductFileService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/idpay/register")
public class ProductFileController {
  private final ProductFileService productFileService;

  public ProductFileController(ProductFileService productFileService) {
    this.productFileService = productFileService;
  }

  @GetMapping("/product-file")
  public ProductFileResponseDTO downloadListUpload(
    @RequestHeader("X-Organization-Id") String idOrg,
    Pageable pageable) {

    return productFileService.downloadFilesByPage(idOrg, pageable);
  }

}
