package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.dto.operation.ProductListDTO;
import org.springframework.data.domain.Pageable;

public interface ProductService  {

  ProductListDTO getProducts (String organizationId,
                              String category,
                              String productCode,
                              String productFileId,
                              String eprelCode,
                              String gtinCode,
                              Pageable pageable);

}
