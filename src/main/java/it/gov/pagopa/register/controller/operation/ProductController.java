package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.dto.operation.ProductUpdateStatusRequestDTO;
import it.gov.pagopa.register.dto.operation.UpdateResultDTO;
import it.gov.pagopa.register.enums.ProductCategories;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.service.operation.ProductService;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static it.gov.pagopa.register.constants.ValidationConstants.*;

@Validated
@RestController
@RequestMapping("/idpay/register")
public class ProductController {


  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping("/product-files/search")
  public ResponseEntity<?> searchProductFiles(
    @RequestParam(required = false)
    @Pattern(regexp = UUID_V4_PATTERN)
    String organizationId,

    @RequestParam(required = false)
    @Pattern(regexp = ANY_TEXT)
    String productName,

    @RequestParam(required = false)
    @Pattern(regexp = OBJECT_ID_PATTERN)
    String productFileId,

    @RequestParam(required = false)
    @Nullable
    @Pattern(regexp = DIGITS_ONLY)
    String eprelCode,

    @RequestParam(required = false)
    @Pattern(regexp = GTIN_CODE)
    String gtinCode,

    @RequestParam(required = false)
    ProductStatus status,

    @RequestParam(required = false)
    ProductCategories category,

    @PageableDefault(size = 20, sort = "registrationDate", direction = Sort.Direction.DESC) Pageable pageable) {
    ProductListDTO result = productService.getProducts(
      organizationId,
      category.name(),
      productFileId,
      eprelCode,
      gtinCode,
      productName,
      status.name(),
      pageable);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/products/update-status")
  public ResponseEntity<UpdateResultDTO> updateProductsState(
    @RequestHeader("x-organization-id")
    @Pattern(regexp = UUID_V4_PATTERN)
      String organizationId,

    @RequestBody ProductUpdateStatusRequestDTO dto) {
    return ResponseEntity.ok(productService.updateProductState(
      organizationId,
      dto.getGtinCodes(),
      dto.getStatus().name(),
      dto.getMotivation())
    );
  }
}
