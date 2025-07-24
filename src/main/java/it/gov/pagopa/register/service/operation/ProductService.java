package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.enums.ProductStatusEnum;
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
                                    String productCode,
                                    String productFileId,
                                    String eprelCode,
                                    String gtinCode,
                                    Pageable pageable) {

    log.info("[GET_PRODUCTS] - Fetching products for organizationId: {}, category: {}, productCode: {}, productFileId: {}, eprelCode: {}, gtinCode: {}",
      organizationId, category, productCode, productFileId, eprelCode, gtinCode);

    Criteria criteria = productRepository.getCriteria(organizationId, category, productCode, productFileId, eprelCode, gtinCode);

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

  public ProductListDTO updateProductStatuses (String organizationId, List<String> productIds, ProductStatusEnum newStatus) {
    log.info("[UPDATE_PRODUCT_STATUSES] - Updating status to {} for products: {}", newStatus, productIds);

    List<Product> products = productRepository.findByIdsAndOrganizationId(productIds, organizationId);

    if (products.size() != productIds.size()) {
      throw new IllegalArgumentException("Alcuni prodotti non esistono o non appartengono all’organizzazione specificata");
    }

    products.forEach(p -> p.setStatus(newStatus.name()));

    List<Product> updated = productRepository.saveAll(products);

    List<ProductDTO> result = updated.stream()
      .map(ProductMapper::toDTO)
      .toList();

    return ProductListDTO.builder()
      .content(result)
      .pageNo(0)
      .pageSize(result.size())
      .totalElements((long) result.size())
      .totalPages(1)
      .build();
  }


}
