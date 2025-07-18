package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProductListDTO;
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

  @GetMapping("/products/filter-by-status")
  public ResponseEntity<ProductListDTO> getProductsByStatus(
    @RequestHeader("x-organization-id") String organizationId,
    @RequestParam(required = false) String category,
    @RequestParam(required = false) String productGroup,
    @RequestParam(required = false) String brand,
    @RequestParam(defaultValue = "false") boolean onlyMarked,
    @PageableDefault(size = 20, sort = "registrationDate", direction = Sort.Direction.DESC) Pageable pageable) {

    ProductListDTO result = productService.getProductsByMarkedStatus(
      onlyMarked,
      organizationId,
      category,
      productGroup,
      brand,
      pageable);

    return ResponseEntity.ok(result);
  }





}
