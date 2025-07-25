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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    List<Product> productsToUpdate = productRepository.findByIdsAndOrganizationId(productIds, organizationId);

    productsToUpdate.forEach(product -> {
      product.setStatus(newStatus.name());
      product.setMotivation(motivation);
    });

    List<Product> productsUpdated = productRepository.saveAll(productsToUpdate);

    Map<String, List<String>> productFileIdToGtins = productsUpdated.stream()
      .collect(Collectors.groupingBy(
        Product::getProductFileId,
        Collectors.mapping(Product::getGtinCode, Collectors.toList())
      ));

    Map<String, List<String>> userEmailToFileIds = new HashMap<>();
    productFileIdToGtins.keySet().forEach(fileId ->
      productFileRepository.findById(fileId).ifPresent(file ->
        userEmailToFileIds
          .computeIfAbsent(file.getUserEmail(), k -> new ArrayList<>())
          .add(fileId)
      )
    );

    Map<String, List<String>> userEmailToGtins = new HashMap<>();
    userEmailToFileIds.forEach((email, fileIds) -> {
      List<String> gtins = fileIds.stream()
        .flatMap(fileId -> productFileIdToGtins.getOrDefault(fileId, List.of()).stream())
        .toList();
      userEmailToGtins.put(email, gtins);
    });

    userEmailToGtins.forEach((email, gtins) ->
      notificationService.sendEmailUpdateStatus(gtins, motivation, newStatus.name(), email)
    );

    return ProductListDTO.builder()
      .pageNo(0)
      .totalPages(1)
      .build();
  }



}
