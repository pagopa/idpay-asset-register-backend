package it.gov.pagopa.register.controller.role;

import it.gov.pagopa.register.dto.ProductListDTO;
import it.gov.pagopa.register.service.role.ProductService;
import it.gov.pagopa.register.service.role.ProductServiceImpl;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
public class ProductControllerImpl implements ProductController {


  private final ProductService productService;

  public ProductControllerImpl(ProductService productService) {
    this.productService = productService;
  }



  @Override
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
