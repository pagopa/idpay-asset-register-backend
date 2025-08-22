package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.dto.operation.EmailProductDTO;
import it.gov.pagopa.register.enums.ProductStatus;
import it.gov.pagopa.register.enums.UserRole;
import it.gov.pagopa.register.model.operation.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.*;

import static it.gov.pagopa.register.constants.AggregationConstants.*;

@RequiredArgsConstructor
public class ProductSpecificRepositoryImpl implements ProductSpecificRepository {


  private final MongoTemplate mongoTemplate;

  @Override
  public List<Product> findByFilter(Criteria criteria, Pageable pageable) {
    Aggregation aggregation = buildAggregation(criteria, pageable);
    return aggregation != null
      ? aggregateResults(aggregation)
      : mongoTemplate.find(Query.query(criteria).with(pageable), Product.class);
  }

  @Override
  public Criteria getCriteria(String organizationId,
                              String category,
                              String productFileId,
                              String eprelCode,
                              String gtinCode,
                              String productName,
                              String status) {

    Criteria criteria = new Criteria();

    if (organizationId != null) {
      criteria.and(Product.Fields.organizationId).is(organizationId);
    }
    if (category != null) {
      criteria.and(Product.Fields.category).is(category);
    }
    if (productFileId != null) {
      criteria.and(Product.Fields.productFileId).is(productFileId);
    }
    if (eprelCode != null) {
      criteria.and(Product.Fields.eprelCode).regex(".*" + eprelCode + ".*", "i");
    }
    if (gtinCode != null) {
      criteria.and(FIELD_ID).regex(".*" + gtinCode + ".*", "i");
    }
    if (productName != null) {
      criteria.and(Product.Fields.productName).regex(".*" + productName + ".*", "i");
    }
    if (status != null) {
      criteria.and(Product.Fields.status).is(status);
    }

    return criteria;
  }

  @Override
  public Long getCount(Criteria criteria) {
    Query query = new Query().addCriteria(criteria);
    return mongoTemplate.count(query, Product.class);
  }

  @Override
  public List<Product> retrieveDistinctProductFileIdsBasedOnRole(String organizationId, String organizationSelected, String role) {
    Criteria criteria = buildRoleBasedCriteria(organizationId, organizationSelected, role);

    List<AggregationOperation> operations = new ArrayList<>();
    if (criteria != null) {
      operations.add(Aggregation.match(criteria));
    }

    operations.add(Aggregation.group(FIELD_PRODUCT_FILE_ID, FIELD_CATEGORY));
    operations.add(Aggregation.project()
      .and("_id." + FIELD_PRODUCT_FILE_ID).as(FIELD_PRODUCT_FILE_ID)
      .and("_id." + FIELD_CATEGORY).as(FIELD_CATEGORY)
    );

    Aggregation aggregation = Aggregation.newAggregation(operations);
    AggregationResults<Product> results = mongoTemplate.aggregate(aggregation, PRODUCT_COLLECTION_NAME, Product.class);
    return results.getMappedResults();
  }

  @Override
  public List<EmailProductDTO> getProductNamesGroupedByEmail(List<String> gtinCodes) {
    Aggregation aggregation = Aggregation.newAggregation(
      Aggregation.addFields()
        .addField("productFileIdObj")
        .withValue(ConvertOperators.ToObjectId.toObjectId("$productFileId"))
        .build(),
      Aggregation.match(Criteria.where("_id").in(gtinCodes)),
      Aggregation.lookup("product_file", "productFileIdObj", "_id", "fileInfo"),
      Aggregation.unwind("fileInfo"),
      Aggregation.group("fileInfo.userEmail")
        .addToSet("productName").as("productNames")
    );

    AggregationResults<EmailProductDTO> results =
      mongoTemplate.aggregate(aggregation, "product", EmailProductDTO.class);

    return results.getMappedResults();
  }


  @Override
  public List<Product> findUpdatableProducts(List<String> productIds, ProductStatus currentStatus, ProductStatus targetStatus, String role) {
    List<String> allowedStates = getAllowedInitialStates(targetStatus, role);
    if (allowedStates.isEmpty() || !allowedStates.contains(currentStatus.name())) {
      return List.of();
    }

    Criteria criteria = new Criteria()
      .and(FIELD_ID).in(productIds)
      .and(FIELD_STATUS).is(currentStatus.name());

    return mongoTemplate.find(Query.query(criteria), Product.class);
  }

  private Aggregation buildAggregation(Criteria criteria, Pageable pageable) {
    Sort sort = pageable.getSort();

    if (isSortedBy(sort, FIELD_ENERGY_CLASS)) {
      return buildEnergyClassAggregation(criteria, pageable);
    }

    if (isSortedBy(sort, FIELD_CATEGORY) || isSortedBy(sort, FIELD_BATCH_NAME)) {
      return buildCategoryAggregation(criteria, pageable, isSortedBy(sort, FIELD_BATCH_NAME));
    }

    return null;
  }

  private boolean isSortedBy(Sort sort, String property) {
    return sort.stream().anyMatch(order -> order.getProperty().equalsIgnoreCase(property));
  }

  private List<Product> aggregateResults(Aggregation aggregation) {
    return mongoTemplate.aggregate(aggregation, PRODUCT_COLLECTION_NAME, Product.class).getMappedResults();
  }

  private Aggregation buildEnergyClassAggregation(Criteria criteria, Pageable pageable) {
    Sort.Direction direction = getSortDirection(pageable, FIELD_ENERGY_CLASS);

    return Aggregation.newAggregation(
      addEnergyRankField(),
      Aggregation.match(criteria),
      Aggregation.sort(Sort.by(direction, "energyRank")),
      Aggregation.skip(pageable.getOffset()),
      Aggregation.limit(pageable.getPageSize())
    );
  }

  private AggregationOperation addEnergyRankField() {
    return Aggregation.addFields()
      .addField("energyRank")
      .withValue(
        ConditionalOperators.switchCases(
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_ENERGY_CLASS).equalToValue("A+++")).then(10),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_ENERGY_CLASS).equalToValue("A++")).then(9),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_ENERGY_CLASS).equalToValue("A+")).then(8),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_ENERGY_CLASS).equalToValue("A")).then(7),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_ENERGY_CLASS).equalToValue("B")).then(6),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_ENERGY_CLASS).equalToValue("C")).then(5),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_ENERGY_CLASS).equalToValue("D")).then(4),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_ENERGY_CLASS).equalToValue("E")).then(3),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_ENERGY_CLASS).equalToValue("F")).then(2)
        ).defaultTo(1)
      ).build();
  }

  private Aggregation buildCategoryAggregation(Criteria criteria, Pageable pageable, boolean sortByBatchName) {
    List<Sort.Order> orders = new ArrayList<>();

    if (sortByBatchName) {
      orders.add(new Sort.Order(getSortDirection(pageable, FIELD_BATCH_NAME), RUNTIME_FIELD_CATEGORY_IT));
      orders.add(new Sort.Order(getSortDirection(pageable, FIELD_BATCH_NAME), FIELD_PRODUCT_FILE_ID));
    } else {
      orders.add(new Sort.Order(getSortDirection(pageable, FIELD_CATEGORY), RUNTIME_FIELD_CATEGORY_IT));
    }

    return Aggregation.newAggregation(
      addCategoryTranslationField(),
      Aggregation.match(criteria),
      Aggregation.sort(Sort.by(orders)),
      Aggregation.skip(pageable.getOffset()),
      Aggregation.limit(pageable.getPageSize())
    );
  }

  private AggregationOperation addCategoryTranslationField() {
    return Aggregation.addFields()
      .addField(RUNTIME_FIELD_CATEGORY_IT)
      .withValue(
        ConditionalOperators.switchCases(
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_CATEGORY).equalToValue("WASHINGMACHINES")).then("Lavatrici"),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_CATEGORY).equalToValue("WASHERDRIERS")).then("Lavasciuga"),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_CATEGORY).equalToValue("OVENS")).then("Forni"),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_CATEGORY).equalToValue("RANGEHOODS")).then("Cappe"),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_CATEGORY).equalToValue("DISHWASHERS")).then("Lavastoviglie"),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_CATEGORY).equalToValue("TUMBLEDRYERS")).then("Asciugatrici"),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_CATEGORY).equalToValue("REFRIGERATINGAPPL")).then("Frigoriferi"),
          ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(FIELD_CATEGORY).equalToValue("COOKINGHOBS")).then("Piani cottura")
        )
      ).build();
  }

  private Sort.Direction getSortDirection(Pageable pageable, String property) {
    return Optional.of(pageable.getSort().getOrderFor(property))
      .map(Sort.Order::getDirection)
      .orElse(Sort.Direction.ASC);
  }

  private Criteria buildRoleBasedCriteria(String organizationId, String organizationSelected, String role) {
    if (UserRole.OPERATORE.getRole().equalsIgnoreCase(role)) {
      return Criteria.where(FIELD_ORGANIZATION_ID).is(organizationId);
    } else if (organizationSelected != null) {
      return Criteria.where(FIELD_ORGANIZATION_ID).is(organizationSelected);
    }
    return null;
  }

  private List<String> getAllowedInitialStates(ProductStatus targetStatus, String role) {
    Map<String, List<String>> validInitialStates = new HashMap<>();

    if (UserRole.INVITALIA.getRole().equalsIgnoreCase(role)) {
      switch (targetStatus) {
        case ProductStatus.WAIT_APPROVED, ProductStatus.REJECTED  ->
          validInitialStates.put(targetStatus.name(),
            List.of(ProductStatus.UPLOADED.name(),ProductStatus.SUPERVISED.name()));
        case ProductStatus.SUPERVISED ->
          validInitialStates.put(targetStatus.name(),
            List.of(ProductStatus.UPLOADED.name()));
        default -> validInitialStates.put(targetStatus.name(), List.of());
      }
    } else if (UserRole.INVITALIA_ADMIN.getRole().equalsIgnoreCase(role)) {
      switch (targetStatus) {
        case ProductStatus.APPROVED, ProductStatus.UPLOADED ->
          validInitialStates.put(targetStatus.name(),
            List.of(ProductStatus.WAIT_APPROVED.name()));
          default -> validInitialStates.put(targetStatus.name(), List.of());
      }
    }
    return validInitialStates.getOrDefault(targetStatus.name(), List.of());
  }


}


