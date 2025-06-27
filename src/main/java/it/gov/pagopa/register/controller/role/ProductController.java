package it.gov.pagopa.register.controller.role;

import it.gov.pagopa.register.dto.ProductListDTO;
import it.gov.pagopa.register.service.role.ProductService;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/products")
public class ProductController {


  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }



  @GetMapping("/")
  public ResponseEntity<ProductListDTO> getProductList(@RequestHeader @NotNull String organizationId,
                                                      @RequestParam String category,
                                                      @RequestParam String productCode,
                                                      @RequestParam String productFileId,
                                                      @RequestParam String eprelCode,
                                                      @RequestParam String gtinCode,
                                                      @RequestParam Pageable pageable){

    ProductListDTO result = productService.getProducts(   organizationId,
                                                          category,
                                                          productCode,
                                                          productFileId,
                                                          eprelCode,
                                                          gtinCode,
                                                          pageable);


    return ResponseEntity.ok(result);


  }
}
