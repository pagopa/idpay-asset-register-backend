package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProductFileResponseDTO;
import it.gov.pagopa.register.service.operation.ProductFileService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/idpay/register")
public class ProductFileController {
  private final ProductFileService productFileService;

  public ProductFileController(ProductFileService productFileService) {
    this.productFileService = productFileService;
  }

  @GetMapping("/listUpload")
  public ProductFileResponseDTO downloadListUpload(
    @RequestHeader String idOrg,
    @RequestParam int page,
    @RequestParam int size) {

    return productFileService.downloadFilesByPage(idOrg, page, size);
  }
}
