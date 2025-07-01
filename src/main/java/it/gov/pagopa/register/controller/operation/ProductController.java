package it.gov.pagopa.register.controller.operation;

import it.gov.pagopa.register.dto.ProductListDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;


public interface ProductController {


  ResponseEntity<ProductListDTO> getProductList(  String organizationId,
                                                  String category,
                                                  String productCode,
                                                  String productFileId,
                                                  String eprelCode,
                                                  String gtinCode,
                                                  Pageable pageable);
}
