package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.mapper.operation.ProductMapper;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ProductService{

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

    Criteria criteria = productRepository.getCriteria(organizationId, category, productCode, productFileId, eprelCode, gtinCode);

    List<Product> entities = productRepository.findByFilter( criteria, pageable);
    Long count = productRepository.getCount(criteria);

    final Page<Product> entitiesPage = PageableExecutionUtils.getPage(entities,
      pageable, () -> count);

    Page<ProductDTO> result = entitiesPage.map(ProductMapper::toDTO);

    return ProductListDTO.builder()
      .content(result.getContent())
      .pageNo(result.getNumber())
      .pageSize(result.getSize())
      .totalElements(result.getTotalElements())
      .totalPages(result.getTotalPages())
      .build();
  }

}
