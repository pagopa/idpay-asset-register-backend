package it.gov.pagopa.register.controller.role;

import it.gov.pagopa.register.dto.ProductListDTO;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public interface ProductController {

  @GetMapping("/")
  ResponseEntity<ProductListDTO> getProductList(@RequestHeader @NotNull String organizationId,
                                                       @RequestParam String category,
                                                       @RequestParam String productCode,
                                                       @RequestParam String productFileId,
                                                       @RequestParam String eprelCode,
                                                       @RequestParam String gtinCode,
                                                       @RequestParam Pageable pageable);
}
