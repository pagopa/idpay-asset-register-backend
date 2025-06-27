package it.gov.pagopa.register.service.role;

import it.gov.pagopa.register.dto.ProductDTO;
import it.gov.pagopa.register.dto.ProductListDTO;
import it.gov.pagopa.register.dto.mapper.role.ProductMapper;
import it.gov.pagopa.register.model.role.Product;
import it.gov.pagopa.register.repository.role.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

  private final ProductRepository productRepository;

  public ProductService(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  public ProductListDTO getProducts (String organizationId,
                                     String category,
                                     String productCode,
                                     String productFileId,
                                     String eprelCode,
                                     String gtinCode,
                                     Pageable pageable){

    Page<Product>entities = productRepository.findProducts( organizationId,
                                                            category,
                                                            productCode,
                                                            productFileId,
                                                            eprelCode,
                                                            gtinCode,
                                                            pageable);

    Page<ProductDTO> result = entities.map(ProductMapper::toDTO);


    return ProductListDTO.builder()
      .content(result.getContent())
      .totalElements(result.getTotalElements())
      .build();
  }
}
