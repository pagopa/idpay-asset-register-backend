package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.service.operation.ProductService;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    @PageableDefault(size = 10, sort = "registrationDate", direction = Sort.Direction.DESC) Pageable pageable) {

    Pageable resolvedPageable = resolveSort(pageable);

    ProductListDTO result = productService.getProducts(
      organizationId,
      category,
      productCode,
      productFileId,
      eprelCode,
      gtinCode,
      resolvedPageable);

    return ResponseEntity.ok(result);
  }


  private Pageable resolveSort(Pageable pageable) {
    Sort.Order order = pageable.getSort().getOrderFor("batchName");
    if (order == null) {
      return pageable;
    }

    Sort newSort = Sort.by(order.isAscending()
      ? List.of(
      Sort.Order.asc("category"),
      Sort.Order.asc("productFileId"))
      : List.of(
      Sort.Order.desc("category"),
      Sort.Order.desc("productFileId")));

    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), newSort);
  }
}
