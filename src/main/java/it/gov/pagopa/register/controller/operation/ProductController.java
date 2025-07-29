package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.dto.operation.ProductUpdateStatusRequestDTO;
import it.gov.pagopa.register.service.operation.ProductService;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/idpay/register")
public class ProductController {


  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping("/products")
  public ResponseEntity<ProductListDTO> getProductList(
    @RequestHeader("x-organization-id") String organizationId,
    @RequestParam @Nullable String category,
    @RequestParam @Nullable String productFileId,
    @RequestParam @Nullable String eprelCode,
    @RequestParam @Nullable String gtinCode,
    @RequestParam @Nullable String productName,
    @RequestParam @Nullable String status,
    @PageableDefault(size = 20, sort = "registrationDate", direction = Sort.Direction.DESC) Pageable pageable) {

    ProductListDTO result = productService.getProducts(
      organizationId,
      category,
      productFileId,
      eprelCode,
      gtinCode,
      productName,
      status,
      pageable);

    return ResponseEntity.ok(result);
  }

  @PatchMapping("/products/update-status")
  public ResponseEntity<ProductListDTO> updateProductsState(
    @RequestHeader("x-organization-id") String organizationId,
    @RequestBody ProductUpdateStatusRequestDTO dto) {

    ProductListDTO result = productService.updateProductState(
      organizationId,
      dto.getGtinCodes(),
      dto.getStatus(),
      dto.getMotivation());
    return ResponseEntity.ok(result);
  }
}
