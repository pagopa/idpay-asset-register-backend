package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.mapper.operation.ProductMapper;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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


  public ProductListDTO getProducts(String organizationId,
                                    String category,
                                    String productFileId,
                                    String eprelCode,
                                    String gtinCode,
                                    String productName,
                                    String status,
                                    Pageable pageable) {

    log.info("[GET_PRODUCTS] - Fetching products for organizationId: {}, category: {}, productFileId: {}, eprelCode: {}, gtinCode: {}, productName: {}, status: {}",
      organizationId, category, productFileId, eprelCode, gtinCode,productName,status);

    Criteria criteria = productRepository.getCriteria(organizationId, category, productFileId, eprelCode, gtinCode,productName, status);

    List<Product> entities = productRepository.findByFilter(criteria, pageable);
    Long count = productRepository.getCount(criteria);

    log.info("[GET_PRODUCTS] - Found {} products matching criteria", count);

    final Page<Product> entitiesPage = PageableExecutionUtils.getPage(entities, pageable, () -> count);

    Page<ProductDTO> result = entitiesPage.map(ProductMapper::toDTO);

    log.info("[GET_PRODUCTS] - Returning {} products for page {} of size {}", result.getTotalElements(), result.getNumber(), result.getSize());

    return ProductListDTO.builder()
      .content(result.getContent())
      .pageNo(result.getNumber())
      .pageSize(result.getSize())
      .totalElements(result.getTotalElements())
      .totalPages(result.getTotalPages())
      .build();
  }


}
