package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.enums.ProductStatusEnum;
import it.gov.pagopa.register.service.operation.ProductService;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
    @RequestParam @Nullable String productCode,
    @RequestParam @Nullable String productFileId,
    @RequestParam @Nullable String eprelCode,
    @RequestParam @Nullable String gtinCode,
    @PageableDefault(size = 20, sort = "registrationDate", direction = Sort.Direction.DESC) Pageable pageable) {

    ProductListDTO result = productService.getProducts(
      organizationId,
      category,
      productCode,
      productFileId,
      eprelCode,
      gtinCode,
      pageable);

    return ResponseEntity.ok(result);
  }

  @PatchMapping("/products/status/approve")
  public ResponseEntity<ProductListDTO> approveProducts(
    @RequestHeader("x-organization-id") String organizationId,
    @RequestBody List<String> productIds) {

    ProductListDTO result = productService.updateProductStatuses(organizationId, productIds, ProductStatusEnum.APPROVED);
    return ResponseEntity.ok(result);
  }

  @PatchMapping("/products/status/validate")
  public ResponseEntity<ProductListDTO> validateProducts(
    @RequestHeader("x-organization-id") String organizationId,
    @RequestBody List<String> productIds) {

    ProductListDTO result = productService.updateProductStatuses(organizationId, productIds, ProductStatusEnum.IN_VALIDATION);
    return ResponseEntity.ok(result);
  }

  @PatchMapping("/products/status/reject")
  public ResponseEntity<ProductListDTO> rejectProducts(
    @RequestHeader("x-organization-id") String organizationId,
    @RequestBody List<String> productIds) {

    ProductListDTO result = productService.updateProductStatuses(organizationId, productIds, ProductStatusEnum.REJECTED);
    return ResponseEntity.ok(result);
  }


}
