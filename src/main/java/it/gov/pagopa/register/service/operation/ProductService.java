package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.connector.notification.NotificationService;
import it.gov.pagopa.register.constants.AssetRegisterConstants;
import it.gov.pagopa.register.dto.operation.EmailProductDTO;
import it.gov.pagopa.register.dto.operation.ProductDTO;
import it.gov.pagopa.register.dto.operation.ProductListDTO;
import it.gov.pagopa.register.dto.operation.UpdateResultDTO;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.mapper.operation.ProductMapper;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.StatusChangeEvent;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class ProductService {

  private final ProductRepository productRepository;
  private final NotificationService notificationService;

  public ProductService(ProductRepository productRepository, NotificationService notificationService) {
    this.productRepository = productRepository;
    this.notificationService = notificationService;
  }

  @SuppressWarnings("java:S107")
  public ProductListDTO fetchProductsByFilters(
    String organizationId,
    String category,
    String productFileId,
    String eprelCode,
    String gtinCode,
    String productName,
    String status,
    Pageable pageable,
    String role
  ) {
    log.info("[GET_PRODUCTS] - Fetching products for organizationId: {}, category: {}, productFileId: {}, eprelCode: {}, gtinCode: {}, productName: {}, status: {}",
      organizationId, category, productFileId, eprelCode, gtinCode,productName,status);

    Criteria criteria = productRepository.getCriteria(organizationId, category, productFileId, eprelCode, gtinCode,productName, status);

    List<Product> entities = productRepository.findByFilter(criteria, pageable);
    Long count = productRepository.getCount(criteria);

    log.info("[GET_PRODUCTS] - Found {} products matching criteria", count);

    Page<Product> entitiesPage = PageableExecutionUtils.getPage(entities, pageable, () -> count);
    Page<ProductDTO> result = entitiesPage.map(p -> ProductMapper.toDTO(p, role));

    log.info("[GET_PRODUCTS] - Returning {} products for page {} of size {}", result.getTotalElements(), result.getNumber(), result.getSize());

    return buildProductListDTO(result);
  }

  public UpdateResultDTO updateProductStatusesWithNotification(
    List<String> productIds,
    ProductStatus currentStatus,
    ProductStatus targetStatus,
    String motivation,
    String role,
    String username
  ) {
    log.info("[UPDATE_PRODUCT_STATUSES] - Starting update - newStatus: {}, motivation: {}", targetStatus, motivation);
    log.debug("[UPDATE_PRODUCT_STATUSES] - Product IDs to update: {}", productIds);

    List<Product> productsToUpdate = productRepository.findUpdatableProducts(productIds, currentStatus, targetStatus, role);
    log.debug("[UPDATE_PRODUCT_STATUSES] - Retrieved {} products for update", productsToUpdate.size());

    updateStatuses(productsToUpdate, targetStatus, motivation,currentStatus,targetStatus,role,username);
    List<Product> productsUpdated = productRepository.saveAll(productsToUpdate);

    log.info("[UPDATE_PRODUCT_STATUSES] - Successfully updated {} products", productsUpdated.size());

    if(targetStatus.name().equals(ProductStatus.REJECTED.toString())) {
      int failedEmails = notifyStatusUpdates(productsUpdated, targetStatus, motivation);
      if (failedEmails != 0) {
        log.warn("[UPDATE_PRODUCT_STATUSES] - Some email notifications failed. Total failures: {}", failedEmails);
        return UpdateResultDTO.ko(AssetRegisterConstants.UpdateKeyConstant.EMAIL_ERROR_KEY);
      }
      log.info("[UPDATE_PRODUCT_STATUSES] - All notifications sent successfully");
    }
    return UpdateResultDTO.ok();
  }


  private void updateStatuses(List<Product> products,
                              ProductStatus newStatus,
                              String motivation,
                              ProductStatus currentStatus,
                              ProductStatus targetStatus,
                              String role,
                              String username
  ) {
    products.forEach(product -> {
      log.debug("[UPDATE_PRODUCT_STATUSES] - Updating product {} status from {} to {}",
        product.getGtinCode(), product.getStatus(), newStatus.name());
      product.setStatus(newStatus.name());
      product.getStatusChangeChronology().add(StatusChangeEvent.builder()
        .username(username)
        .role(role.equals(UserRole.INVITALIA.getRole()) ? "L1" : "L2")
        .updateDate(LocalDateTime.now())
        .currentStatus(currentStatus)
        .targetStatus(targetStatus)
        .motivation(motivation)
        .build());
    });
  }

  private int notifyStatusUpdates(List<Product> products, ProductStatus newStatus, String motivation) {
    List<EmailProductDTO>  emailToProducts = productRepository.getProductNamesGroupedByEmail(
      products.stream().map(Product::getGtinCode).toList()
    );

    List<String> failedEmails = new ArrayList<>();

    for (EmailProductDTO dto : emailToProducts) {
      try {
        notificationService.sendEmailUpdateStatus(
          dto.getProductNames(),
          motivation,
          newStatus.name(),
          dto.getId()
        );
      } catch (Exception e) {
        log.debug("[UPDATE_PRODUCT_STATUSES] - Failed to send email to {}: {}", dto.getId(), e.getMessage());
        failedEmails.add(dto.getId());
      }
    }

    return failedEmails.size();
  }

  private ProductListDTO buildProductListDTO(Page<ProductDTO> result) {
    return ProductListDTO.builder()
      .content(result.getContent())
      .pageNo(result.getNumber())
      .pageSize(result.getSize())
      .totalElements(result.getTotalElements())
      .totalPages(result.getTotalPages())
      .build();
  }
}

