package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.dto.operation.ProductUpdateStatusRequestDTO;
import it.gov.pagopa.register.dto.operation.UpdateResultDTO;
import it.gov.pagopa.register.enums.ProductCategories;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.service.operation.ProductService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static it.gov.pagopa.register.constants.ValidationPatterns.*;

@Validated
@RestController
@RequestMapping("/idpay/register")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  @GetMapping("/products")
  public ResponseEntity<ProductListDTO> getProductList(
    @RequestHeader(value = "x-organization-role", required = false, defaultValue = "operatore") @Pattern(regexp = ROLE_PATTERN) String role,
    @RequestParam(required = false) @Pattern(regexp = UUID_V4_PATTERN) String organizationId,
    @RequestParam(required = false) @Pattern(regexp = ANY_TEXT) String productName,
    @RequestParam(required = false) @Pattern(regexp = OBJECT_ID_PATTERN) String productFileId,
    @RequestParam(required = false) @Pattern(regexp = DIGITS_ONLY) String eprelCode,
    @RequestParam(required = false) @Pattern(regexp = GTIN_CODE) String gtinCode,
    @RequestParam(required = false) ProductStatus status,
    @RequestParam(required = false) ProductCategories category,
    @PageableDefault(size = 20, sort = "registrationDate", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    String categoryName = Optional.ofNullable(category).map(Enum::name).orElse(null);
    String statusName = Optional.ofNullable(status).map(Enum::name).orElse(null);

    ProductListDTO result = productService.fetchProductsByFilters(
      organizationId,
      categoryName,
      productFileId,
      eprelCode,
      gtinCode,
      productName,
      statusName,
      pageable,
      role
    );

    return ResponseEntity.ok(result);
  }

  @PostMapping("/products/update-status")
  public ResponseEntity<UpdateResultDTO> updateProductsState(
    @RequestHeader("x-organization-role") @Pattern(regexp = ROLE_PATTERN) String role,
    @RequestHeader("x-user-name") String username,
    @RequestBody ProductUpdateStatusRequestDTO dto
  ) {
    UpdateResultDTO result = productService.updateProductStatusesWithNotification(
      dto.getGtinCodes(),
      dto.getCurrentStatus(),
      dto.getTargetStatus(),
      dto.getMotivation(),
      dto.getFormalMotivation(),
      role,
      username
    );

    return ResponseEntity.ok(result);
  }
}
