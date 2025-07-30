package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.notification.NotificationService;
import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.dto.operation.UpdateResultDTO;
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


  @SuppressWarnings("java:S107")
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

  public UpdateResultDTO updateProductState(String organizationId, List<String> productIds, ProductStatusEnum newStatus, String motivation) {
    log.info("[UPDATE_PRODUCT_STATUSES] - Starting update for organizationId: {}, newStatus: {}, motivation: {}", organizationId, newStatus, motivation);
    log.debug("[UPDATE_PRODUCT_STATUSES] - Product IDs to update: {}", productIds);

    List<Product> productsToUpdate = productRepository.findByIdsAndOrganizationId(productIds, organizationId);
    log.debug("[UPDATE_PRODUCT_STATUSES] - Retrieved {} products for update", productsToUpdate.size());

    productsToUpdate.forEach(product -> {
      log.debug("[UPDATE_PRODUCT_STATUSES] - Updating product {} status from {} to {}", product.getGtinCode(), product.getStatus(), newStatus.name());
      product.setStatus(newStatus.name());
      product.setMotivation(motivation);
    });

    List<Product> productsUpdated = productRepository.saveAll(productsToUpdate);
    log.info("[UPDATE_PRODUCT_STATUSES] - Successfully updated {} products", productsUpdated.size());

    Map<String, List<String>> productFileIdToGtins = productsUpdated.stream()
      .collect(Collectors.groupingBy(
        Product::getProductFileId,
        Collectors.mapping(Product::getGtinCode, Collectors.toList())
      ));
    log.debug("[UPDATE_PRODUCT_STATUSES] - Grouped GTINs by product file ID: {}", productFileIdToGtins);

    Map<String, List<String>> userEmailToFileIds = new HashMap<>();
    productFileIdToGtins.keySet().forEach(fileId ->
      productFileRepository.findById(fileId).ifPresent(file -> {
        log.debug("[UPDATE_PRODUCT_STATUSES] - Found file {} for user {}", fileId, file.getUserEmail());
        userEmailToFileIds
          .computeIfAbsent(file.getUserEmail(), k -> new ArrayList<>())
          .add(fileId);
      })
    );
    log.debug("[UPDATE_PRODUCT_STATUSES] - Mapped user emails to file IDs: {}", userEmailToFileIds);

    Map<String, List<String>> userEmailToGtins = new HashMap<>();
    userEmailToFileIds.forEach((email, fileIds) -> {
      List<String> gtins = fileIds.stream()
        .flatMap(fileId -> productFileIdToGtins.getOrDefault(fileId, List.of()).stream())
        .toList();
      userEmailToGtins.put(email, gtins);
      log.trace("[UPDATE_PRODUCT_STATUSES] - User {} will be notified for GTINs: {}", email, gtins);
    });

    try{
      userEmailToGtins.forEach((email, gtins) -> {
        log.info("[UPDATE_PRODUCT_STATUSES] - Sending notification to {} for {} products", email, gtins.size());
        notificationService.sendEmailUpdateStatus(gtins, motivation, newStatus.name(), email);
      });
    }
    catch (Exception e){
      log.error("[UPDATE_PRODUCT_STATUSES] Error: {}",e.getMessage());
      return  UpdateResultDTO.ko(AssetRegisterConstants.UpdateKeyConstant.EMAIL_ERROR_KEY);
    }

    log.info("[UPDATE_PRODUCT_STATUSES] - Update process completed");

    return UpdateResultDTO.ok();
  }




}
