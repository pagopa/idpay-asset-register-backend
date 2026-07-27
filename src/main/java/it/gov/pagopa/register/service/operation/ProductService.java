package it.gov.pagopa.register.service.operation;

import it.gov.pagopa.register.configuration.InitiativeConfigMap;
import it.gov.pagopa.register.connector.notification.NotificationService;
import it.gov.pagopa.register.dto.operation.*;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.mapper.operation.ProductMapper;
import it.gov.pagopa.register.model.initiative.InitiativeConfig;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.UpdateError.*;

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
    String initiativeId,
    String category,
    String productFileId,
    String eprelCode,
    String gtinCode,
    String productCode,
    String productName,
    String fullProductName,
    String brand,
    String model,
    String status,
    Pageable pageable,
    String role
  ) {
    log.info("[GET_PRODUCTS] - Fetching products for organizationId: {}, initiativeId: {}, category: {}, productFileId: {}, eprelCode: {}, gtinCode: {}, productName: {}, brand: {}, model: {}, status: {}, sort: {}",
      organizationId, initiativeId, category, productFileId, eprelCode, gtinCode, productName, brand, model, status, pageable.getSort());

    boolean includeWaitApproved = ProductStatus.UPLOADED.name().equals(status)
      && !isInvitaliaRole(role);

    final Criteria criteria = productRepository.getCriteria(
      ProductCriteriaDTO.builder()
        .organizationId(organizationId)
        .initiativeId(initiativeId)
        .category(category)
        .productFileId(productFileId)
        .eprelCode(eprelCode)
        .gtinCode(gtinCode)
        .productCode(productCode)
        .productName(productName)
        .fullProductName(fullProductName)
        .brand(brand)
        .model(model)
        .status(includeWaitApproved ? null : status)
        .build()
    );

    if (includeWaitApproved) {
      criteria.and(Product.Fields.status).in(
        ProductStatus.UPLOADED.name(),
        ProductStatus.WAIT_APPROVED.name()
      );
    }

    List<Product> entities = productRepository.findByFilter(criteria, pageable);
    Long count = productRepository.getCount(criteria);

    log.info("[GET_PRODUCTS] - Found {} products matching criteria", count);

    Page<Product> entitiesPage = PageableExecutionUtils.getPage(entities, pageable, () -> count);
    Page<ProductDTO> result = entitiesPage.map(p -> ProductMapper.toDTO(p, role));

    log.info("[GET_PRODUCTS] - Returning {} products for page {} of size {}", result.getTotalElements(), result.getNumber(), result.getSize());

    return buildProductListDTO(result);
  }

  private boolean isInvitaliaRole(String role) {
    return UserRole.INVITALIA.getRole().equalsIgnoreCase(role)
      || UserRole.INVITALIA_ADMIN.getRole().equalsIgnoreCase(role);
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

  public UpdateResultDTO updateProductStatusesWithNotification(
    String initiativeId,
    ProductUpdateStatusRequestDTO request,
    String role,
    String username
  ) {

    log.info(
      "[UPDATE_PRODUCT_STATUSES] - Starting update - initiativeId: {}, targetStatus: {}, motivation: {}, formalMotivation: {}",
      initiativeId,
      request.getTargetStatus(),
      request.getMotivation(),
      request.getFormalMotivation()
    );

    InitiativeConfig config = initiativeConfigMap.get(initiativeId);
    if(!validConfig(config)){
      return UpdateResultDTO.ko(INITIATIVE_NOT_FOUND_ERROR_KEY);
    }

    if (!isTransitionAllowed(config, role, request.getCurrentStatus(), request.getTargetStatus())) {
      return UpdateResultDTO.ko(TRANSITION_NOT_ALLOWED_ERROR_KEY);
    }

    List<Product> products = getProductsOrFail(request.getGtinCodes(), initiativeId);
    if (products.isEmpty()) {
      return UpdateResultDTO.ko(PRODUCT_NOT_FOUND_ERROR_KEY);
    }

    String currentStatus = validateAndGetUniformStatus(products);
    if (currentStatus == null) {
      return UpdateResultDTO.ko(MIXED_STATUS_ERROR_KEY);
    }

    if (!isRequestStatusCoherent(request, currentStatus)) {
      return UpdateResultDTO.ko(INVALID_CURRENT_STATUS_ERROR_KEY);
    }

    updateProducts(products, request, role, username);

    List<Product> updatedProducts = productRepository.saveAll(products);

    log.info(
      "[UPDATE_PRODUCT_STATUSES] - Successfully updated {} products",
      updatedProducts.size()
    );

    notifyIfRejected(updatedProducts, request, initiativeId);

    return UpdateResultDTO.ok();
  }

  private boolean validConfig(InitiativeConfig config) {
    if (config == null) {
      log.error("[UPDATE_PRODUCT_STATUSES] - Config is null");
      return false;
    }

    return true;
  }

  public boolean isTransitionAllowed(
    InitiativeConfig config,
    String role,
    ProductStatus currentStatus,
    ProductStatus targetStatus
  ) {

    Map<String, Map<String, List<ProductStatus>>> stateTransitions =
      config.getStateTransitions();

    Map<String, List<ProductStatus>> roleTransitions =
      stateTransitions.get(role);

    if (stateTransitions.isEmpty()) {
      log.error(
        "[UPDATE_PRODUCT_STATUSES] - stateTransitions missing or empty for initiative {}",
        config.getInitiativeId()
      );
      return false;
    }
    if (roleTransitions.isEmpty()) {
      log.warn(
        "[UPDATE_PRODUCT_STATUSES] - No transitions configured for role {}",
        role
      );
      return false;
    }

    List<ProductStatus> allowedInitialStates =
      roleTransitions.get(targetStatus.name());

    if (allowedInitialStates == null) {
      log.warn(
        "[UPDATE_PRODUCT_STATUSES] - No transitions configured for role {} and target {}",
        role,
        targetStatus
      );
      return false;
    }

    boolean allowed = allowedInitialStates.contains(currentStatus);

    if (!allowed) {
      log.warn(
        "[UPDATE_PRODUCT_STATUSES] - Transition not allowed [role={}, {} -> {}]",
        role,
        currentStatus,
        targetStatus
      );
    }

    return allowed;
  }
  private List<Product> getProductsOrFail(List<String> gtins, String initiativeId) {

    List<Product> products =
      productRepository.findByGtinCodeInAndInitiativeId(gtins, initiativeId);

    if (products.isEmpty() || products.size() != gtins.size()) {
      log.warn("[UPDATE_PRODUCT_STATUSES] - Some products not found or not accessible");
      return Collections.emptyList();
    }

    return products;
  }

  private String validateAndGetUniformStatus(List<Product> products) {

    String firstStatus = products.get(0).getStatus();

    boolean mixed = products.stream()
      .map(Product::getStatus)
      .anyMatch(status -> !status.equals(firstStatus));

    if (mixed) {
      log.warn(
        "[UPDATE_PRODUCT_STATUSES] - Mixed current statuses in request: {}",
        products.stream().map(Product::getStatus).distinct().toList()
      );
      return null;
    }

    return firstStatus;
  }
  private boolean isRequestStatusCoherent(ProductUpdateStatusRequestDTO request, String actualStatus) {

    if (request.getCurrentStatus() == null) {
      return false;
    }

    boolean valid = request.getCurrentStatus().name().equals(actualStatus);

    if (!valid) {
      log.warn(
        "[UPDATE_PRODUCT_STATUSES] - Provided currentStatus ({}) does not match actual ({})",
        request.getCurrentStatus(),
        actualStatus
      );
    }

    return valid;
  }

  private void updateProducts(
    List<Product> products,
    ProductUpdateStatusRequestDTO request,
    String role,
    String username
  ) {

    LocalDateTime now = LocalDateTime.now();

    for (Product product : products) {

      log.debug(
        "[UPDATE_PRODUCT_STATUSES] - Updating product {} status from {} to {}",
        product.getGtinCode(),
        product.getStatus(),
        request.getTargetStatus()
      );

      product.setStatus(request.getTargetStatus().name());

      if (StringUtils.isNotBlank(request.getFormalMotivation())) {
        product.setFormalMotivation(request.getFormalMotivation());
      }

      if (product.getStatusChangeChronology() == null) {
        product.setStatusChangeChronology(new ArrayList<>());
      }

      product.getStatusChangeChronology().add(
        buildStatusChangeEvent(request, role, username, now)
      );
    }
  }
  private StatusChangeEvent buildStatusChangeEvent(
    ProductUpdateStatusRequestDTO request,
    String role,
    String username,
    LocalDateTime now
  ) {
    return StatusChangeEvent.builder()
      .username(username)
      .role(mapRole(role))
      .updateDate(now)
      .currentStatus(request.getCurrentStatus())
      .targetStatus(request.getTargetStatus())
      .motivation(request.getMotivation())
      .build();
  }

  private String mapRole(String role) {
    return UserRole.INVITALIA.getRole().equals(role) ? "L1" : "L2";
  }

  private void notifyIfRejected(
    List<Product> products,
    ProductUpdateStatusRequestDTO request,
    String initiativeId
  ) {

    if (request.getTargetStatus() != ProductStatus.REJECTED) {
      return;
    }

    int failedEmails = notifyStatusUpdates(
      products,
      request.getTargetStatus(),
      request.getFormalMotivation(),
      initiativeId

    );

    if (failedEmails > 0) {
      log.warn(
        "[UPDATE_PRODUCT_STATUSES] - Some email notifications failed. Total failures: {}",
        failedEmails
      );
    }
  }
  private int notifyStatusUpdates(
    List<Product> products,
    ProductStatus newStatus,
    String formalMotivation,
    String initiativeId
  ) {

    List<String> gtinCodes = products.stream()
      .map(Product::getGtinCode)
      .toList();

    List<EmailProductDTO> emailToProducts =
      productRepository.getProductNamesGroupedByEmail(gtinCodes, initiativeId);

    List<String> failedEmails = new ArrayList<>();

    for (EmailProductDTO dto : emailToProducts) {

      String email = dto.getEmail();

      if (email.equals("null")) {
        log.warn(
          "[UPDATE_PRODUCT_STATUSES] - Skipping products {} because email is null",
          dto.getProductNames()
        );
        continue;
      }

      try {
        notificationService.sendEmailUpdateStatus(
          dto.getProductNames(),
          formalMotivation,
          newStatus.name(),
          email
        );
      } catch (Exception e) {
        log.debug(
          "[UPDATE_PRODUCT_STATUSES] - Failed to send email to {}: {}",
          email,
          e.getMessage()
        );
        failedEmails.add(email);
      }
    }

    return failedEmails.size();
  }


}
