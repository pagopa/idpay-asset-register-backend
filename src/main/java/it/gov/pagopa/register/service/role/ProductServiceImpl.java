package it.gov.pagopa.register.service.role;

import it.gov.pagopa.register.dto.ProductDTO;
import it.gov.pagopa.register.dto.ProductListDTO;
import it.gov.pagopa.register.dto.mapper.role.ProductMapper;
import it.gov.pagopa.register.model.role.Product;
import it.gov.pagopa.register.repository.role.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;

  public ProductServiceImpl(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @Override
  public ProductListDTO getProducts (String organizationId,
                                     String category,
                                     String productCode,
                                     String productFileId,
                                     String eprelCode,
                                     String gtinCode,
                                     Pageable pageable){

    Criteria criteria = productRepository.getCriteria(organizationId, category, productCode, productFileId, eprelCode, gtinCode);

    Page<Product>entities = productRepository.findByFilter( organizationId,
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
