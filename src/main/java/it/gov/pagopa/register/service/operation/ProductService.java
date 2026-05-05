package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.configuration.initiative.model.InitiativeConfig;
import it.gov.pagopa.register.configuration.initiative.InitiativeConfigMap;
import it.gov.pagopa.register.connector.notification.NotificationService;
import it.gov.pagopa.register.dto.operation.*;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.mapper.operation.ProductMapper;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.StatusChangeEvent;
import it.gov.pagopa.register.repository.operation.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;
import static it.gov.pagopa.register.constants.AssetRegisterConstants.UpdateKeyConstant.EMAIL_ERROR_KEY;

@Slf4j
@Service
public class ProductService {

  private final ProductRepository productRepository;
  private final NotificationService notificationService;
  private final InitiativeConfigMap initiativeConfigMap;

  public ProductService(ProductRepository productRepository, NotificationService notificationService, InitiativeConfigMap initiativeConfigMap) {
    this.productRepository = productRepository;
    this.notificationService = notificationService;
    this.initiativeConfigMap = initiativeConfigMap;
  }

  @SuppressWarnings("java:S107")
  public ProductListDTO fetchProductsByFilters(
    String organizationId,
    String category,
    String productFileId,
    String eprelCode,
    String gtinCode,
    String productName,
    String fullProductName,
    String brand,
    String model,
    String status,
    Pageable pageable,
    String role
  ) {
    log.info("[GET_PRODUCTS] - Fetching products for organizationId: {}, category: {}, productFileId: {}, eprelCode: {}, gtinCode: {}, productName: {}, brand: {}, model: {}, status: {}, sort: {}",
      organizationId, category, productFileId, eprelCode, gtinCode, productName, brand, model, status, pageable.getSort());

    final Criteria criteria = productRepository.getCriteria(
      ProductCriteriaDTO.builder()
        .organizationId(organizationId)
        .category(category)
        .productFileId(productFileId)
        .eprelCode(eprelCode)
        .gtinCode(gtinCode)
        .productName(productName)
        .fullProductName(fullProductName)
        .brand(brand)
        .model(model)
        .status(status)
        .build()
    );

    List<Product> entities = productRepository.findByFilter(criteria, pageable);
    Long count = productRepository.getCount(criteria);

    log.info("[GET_PRODUCTS] - Found {} products matching criteria", count);

    Page<Product> entitiesPage = PageableExecutionUtils.getPage(entities, pageable, () -> count);
    Page<ProductDTO> result = entitiesPage.map(p -> ProductMapper.toDTO(p, role));

    log.info("[GET_PRODUCTS] - Returning {} products for page {} of size {}", result.getTotalElements(), result.getNumber(), result.getSize());

    return buildProductListDTO(result);
  }

  public UpdateResultDTO updateProductStatusesWithNotification(
    ProductUpdateStatusRequestDTO updateStatusDto,
    String role,
    String username
  ) {

    log.info(
      "[UPDATE_PRODUCT_STATUSES] - Starting update - initiativeId: {}, targetStatus: {}, motivation: {}, formalMotivation: {}",
      updateStatusDto.getInitiativeId(),
      updateStatusDto.getTargetStatus(),
      updateStatusDto.getMotivation(),
      updateStatusDto.getFormalMotivation()
    );

    // Recupero configurazione iniziativa
    InitiativeConfig initiativeConfig =
      initiativeConfigMap.get(updateStatusDto.getInitiativeId());

    if (initiativeConfig == null) {
      log.warn(
        "[UPDATE_PRODUCT_STATUSES] - Initiative not found: {}",
        updateStatusDto.getInitiativeId()
      );
      return UpdateResultDTO.ko(INITIATIVE_NOT_FOUND_ERROR_KEY);
    }

    // Recupero prodotti richiesti
    List<Product> requestedProducts =
      productRepository.findByIdsAndInitiativeId(updateStatusDto.getGtinCodes(),updateStatusDto.getInitiativeId());

    if (requestedProducts.size() != updateStatusDto.getGtinCodes().size()) {
      log.warn("[UPDATE_PRODUCT_STATUSES] - Some products not found or not accessible");
      return UpdateResultDTO.ko(PRODUCT_NOT_FOUND_ERROR_KEY);
    }

    // Verifica che tutti i prodotti abbiano lo stesso stato corrente
    String distinctCurrentStatus = requestedProducts.get(0).getStatus();

    boolean mixedStatuses = requestedProducts.stream()
      .map(Product::getStatus)
      .anyMatch(status -> !status.equals(distinctCurrentStatus));

    if (mixedStatuses) {
      log.warn(
        "[UPDATE_PRODUCT_STATUSES] - Mixed current statuses in request: {}",
        requestedProducts.stream().map(Product::getStatus).distinct().toList()
      );
      return UpdateResultDTO.ko(MIXED_STATUS_ERROR_KEY);
    }

    // Verifica che currentStatus dichiarato dal client sia corretto
    if (updateStatusDto.getCurrentStatus() == null ||
      !updateStatusDto.getCurrentStatus().name().equals(distinctCurrentStatus)) {

      log.warn(
        "[UPDATE_PRODUCT_STATUSES] - Provided currentStatus ({}) does not match actual ({})",
        updateStatusDto.getCurrentStatus(),
        distinctCurrentStatus
      );
      return UpdateResultDTO.ko(INVALID_CURRENT_STATUS_ERROR_KEY);
    }

    // Verifica transizione di stato basata su InitiativeConfig
    boolean transitionAllowed =
      initiativeConfig.isTransitionAllowed(
        role,
        updateStatusDto.getCurrentStatus(),
        updateStatusDto.getTargetStatus()
      );

    if (!transitionAllowed) {
      log.warn(
        "[UPDATE_PRODUCT_STATUSES] - Transition not allowed for role {}: {} -> {}",
        role,
        updateStatusDto.getCurrentStatus(),
        updateStatusDto.getTargetStatus()
      );
      return UpdateResultDTO.ko(TRANSITION_NOT_ALLOWED_ERROR_KEY);
    }

    // Aggiornamento stato e storico
    LocalDateTime now = LocalDateTime.now();

    requestedProducts.forEach(product -> {

      log.debug(
        "[UPDATE_PRODUCT_STATUSES] - Updating product {} status from {} to {}",
        product.getGtinCode(),
        product.getStatus(),
        updateStatusDto.getTargetStatus().name()
      );

      product.setStatus(updateStatusDto.getTargetStatus().name());

      if (StringUtils.isNotBlank(updateStatusDto.getFormalMotivation())) {
        product.setFormalMotivation(updateStatusDto.getFormalMotivation());
      }

      if (product.getStatusChangeChronology() == null) {
        product.setStatusChangeChronology(new ArrayList<>());
      }

      product.getStatusChangeChronology().add(
        StatusChangeEvent.builder()
          .username(username)
          .role(
            UserRole.INVITALIA.getRole().equals(role)
              ? "L1"
              : "L2"
          )
          .updateDate(now)
          .currentStatus(updateStatusDto.getCurrentStatus())
          .targetStatus(updateStatusDto.getTargetStatus())
          .motivation(updateStatusDto.getMotivation())
          .build()
      );
    });

    // Persistenza
    List<Product> updatedProducts = productRepository.saveAll(requestedProducts);

    log.info(
      "[UPDATE_PRODUCT_STATUSES] - Successfully updated {} products",
      updatedProducts.size()
    );

    // Notifica (solo per stato REJECTED)
    if (ProductStatus.REJECTED.name().equals(updateStatusDto.getTargetStatus().name())) {

      int failedEmails =
        notifyStatusUpdates(
          updatedProducts,
          updateStatusDto.getTargetStatus(),
          updateStatusDto.getFormalMotivation()
        );

      if (failedEmails > 0) {
        log.warn(
          "[UPDATE_PRODUCT_STATUSES] - Some email notifications failed. Total failures: {}",
          failedEmails
        );
        return UpdateResultDTO.ko(EMAIL_ERROR_KEY);
      }
    }

    return UpdateResultDTO.ok();
  }

  private void updateStatuses(List<Product> products,
                              String role,
                              String username,
                              ProductUpdateStatusRequestDTO updateStatusDto) {

    products.forEach(product -> {
      log.debug("[UPDATE_PRODUCT_STATUSES] - Updating product {} status from {} to {}",
        product.getGtinCode(), product.getStatus(), updateStatusDto.getTargetStatus().name());

      product.setStatus(updateStatusDto.getTargetStatus().name());

      log.debug("[UPDATE_PRODUCT_STATUSES] - RequestDTO formalMotivation {}", updateStatusDto.getFormalMotivation());
      if(StringUtils.isNotBlank(updateStatusDto.getFormalMotivation())){
        product.setFormalMotivation(updateStatusDto.getFormalMotivation());
        log.debug("[UPDATE_PRODUCT_STATUSES] - Updated formalMotivation {}", product.getFormalMotivation());
      }

      if (product.getStatusChangeChronology() == null) {
        product.setStatusChangeChronology(new ArrayList<>());
      }

      product.getStatusChangeChronology().add(StatusChangeEvent.builder()
        .username(username)
        .role(role.equals(UserRole.INVITALIA.getRole()) ? "L1" : "L2")
        .updateDate(LocalDateTime.now())
        .currentStatus(updateStatusDto.getCurrentStatus())
        .targetStatus(updateStatusDto.getTargetStatus())
        .motivation(updateStatusDto.getMotivation())
        .build());
    });
  }

  private int notifyStatusUpdates(List<Product> products, ProductStatus newStatus, String formalMotivation) {
    List<EmailProductDTO> emailToProducts = productRepository.getProductNamesGroupedByEmail(
      products.stream().map(Product::getGtinCode).toList()
    );

    List<String> failedEmails = new ArrayList<>();

    for (EmailProductDTO dto : emailToProducts) {
      try {
        notificationService.sendEmailUpdateStatus(
          dto.getProductNames(),
          formalMotivation,
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
