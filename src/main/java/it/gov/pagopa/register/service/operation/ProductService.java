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

import java.util.ArrayList;
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

    List<Product> allProducts = productRepository.findAllById(productIds);

    List<Product> validProducts = new ArrayList<>();
    List<ProductDTO> rejectedProducts = new ArrayList<>();

    for (Product product : allProducts) {
      if (organizationId.equals(product.getOrganizationId())) {
        product.setStatus(newStatus.name());
        validProducts.add(product);
      } else {
        ProductDTO rejectedDTO = ProductMapper.toDTO(product);
        rejectedDTO.setRejectReason("Prodotto non appartenente all'organizzazione");
        rejectedProducts.add(rejectedDTO);
      }
    }

    List<ProductDTO> updatedDTOs = validProducts.isEmpty() ? new ArrayList<>() :
      productRepository.saveAll(validProducts)
        .stream()
        .map(ProductMapper::toDTO)
        .toList();

    List<ProductDTO> allResults = new ArrayList<>();
    allResults.addAll(updatedDTOs);
    allResults.addAll(rejectedProducts);

    String operationMessage = String.format("Operazione eseguita: aggiornati %d elementi, scartati %d elementi",
      updatedDTOs.size(), rejectedProducts.size());

    return ProductListDTO.builder()
      .content(allResults)
      .pageNo(0)
      .pageSize(allResults.size())
      .totalElements((long) allResults.size())
      .totalPages(2)
      .message(operationMessage)
      .build();
  }


}
