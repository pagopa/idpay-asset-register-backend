package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.notification.NotificationService;
import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.enums.ProductStatusEnum;
import it.gov.pagopa.register.mapper.operation.ProductMapper;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductFileRepository;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.*;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.COOKINGHOBS;

@Service
@Slf4j
public class ProductService{

  private final ProductRepository productRepository;
  private final ProductFileRepository productFileRepository;
  private final NotificationService notificationService;
  public ProductService(ProductRepository productRepository, ProductFileRepository productFileRepository, NotificationService notificationService) {
    this.productRepository = productRepository;
    this.productFileRepository = productFileRepository;
    this.notificationService = notificationService;
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

  public ProductListDTO updateProductState(String organizationId, List<String> productIds, ProductStatusEnum newStatus, String motivation) {
    log.info("[UPDATE_PRODUCT_STATUSES] - Updating status to {} for products: {}", newStatus, productIds);

    List<Product> productsToUpdate = productRepository.findByIdsAndOrganizationId(productIds,organizationId);

    productsToUpdate.forEach(p ->{
      p.setStatus(newStatus.name());
      p.setMotivation(motivation);
    });

    List<Product> productsUpdated = productRepository.saveAll(productsToUpdate);

    Map<String, List<String>> productFileIdMap = new HashMap<>();
    for (Product p : productsUpdated) {
      productFileIdMap
        .computeIfAbsent(p.getProductFileId(), k -> new ArrayList<>())
        .add(p.getGtinCode());
    }

    Map<String, List<String>> emailMap = new HashMap<>();
    for (String fileId : productFileIdMap.keySet()) {
      productFileRepository.findById(fileId).ifPresent(file ->
        emailMap
          .computeIfAbsent(file.getUserEmail(), k -> new ArrayList<>())
          .add(fileId)
      );
    }

    Map<String, List<String>> emailProduct = new HashMap<>();
    for (Map.Entry<String, List<String>> entry : emailMap.entrySet()) {
      String email = entry.getKey();
      List<String> fileIds = entry.getValue();

      List<String> gtinCodes = fileIds.stream()
        .flatMap(fileId -> productFileIdMap.getOrDefault(fileId, List.of()).stream())
        .toList();

      emailProduct.put(email, gtinCodes);
    }

    emailProduct.keySet().forEach(e ->
      notificationService.sendEmailUpdateStatus(emailProduct.get(e).toString(), motivation, newStatus.name(), e)
    );

    return ProductListDTO.builder()
      .pageNo(0)
      .totalPages(2)
      .build();
  }


}
