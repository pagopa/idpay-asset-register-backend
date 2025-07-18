package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.model.operation.Product;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

public class ProductSpecificRepositoryImpl implements ProductSpecificRepository {

  public static final String PRODUCT = "product";
  public static final String CATEGORY = "category";
  public static final String PRODUCT_FILE_ID = "productFileId";
  public static final String BATCH_NAME = "batchName";
  public static final String ORGANIZATION_ID = "organizationId";

  public static final String ENERGY_CLASS = "energyClass";
  private final MongoTemplate mongoTemplate;

  public ProductSpecificRepositoryImpl(MongoTemplate mongoTemplate){
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public List<Product> findByFilter(Criteria criteria, Pageable pageable) {
    Sort sort = pageable.getSort();

    boolean sortByEnergyClass = sort.stream()
      .anyMatch(order -> order.getProperty().equalsIgnoreCase(ENERGY_CLASS));

    if (sortByEnergyClass) {
      return mongoTemplate.aggregate(energyClassAggregation(criteria, pageable), PRODUCT, Product.class).getMappedResults();
    } else {
      Pageable resolvedPageable = resolveSort(pageable);
      return mongoTemplate.find(
        Query.query(criteria).with(this.getPageable(resolvedPageable)),
        Product.class
      );
    }
  }

  private static Aggregation energyClassAggregation(Criteria criteria, Pageable pageable) {
    Sort.Order energyClassOrder = pageable.getSort().getOrderFor(ENERGY_CLASS);
    Sort.Direction direction = energyClassOrder != null ? energyClassOrder.getDirection() : Sort.Direction.ASC;

    return Aggregation.newAggregation(
      Aggregation.addFields()
        .addField("energyRank")
        .withValue(
          ConditionalOperators.switchCases(
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("A+++")).then(1),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("A++")).then(2),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("A+")).then(3),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("A")).then(4),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("B")).then(5),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("C")).then(6),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("D")).then(7),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("E")).then(8),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("F")).then(9)
          ).defaultTo(10)
        ).build(),
      Aggregation.match(criteria),
      Aggregation.sort(Sort.by(direction, "energyRank")),
      Aggregation.skip(pageable.getOffset()),
      Aggregation.limit(pageable.getPageSize())
    );
  }


  private Pageable resolveSort(Pageable pageable) {
    Sort.Order order = pageable.getSort().getOrderFor(BATCH_NAME);
    if (order == null) {
      return pageable;
    }

    Sort newSort = Sort.by(order.isAscending()
      ? List.of(
      Sort.Order.asc(CATEGORY),
      Sort.Order.asc(PRODUCT_FILE_ID))
      : List.of(
      Sort.Order.desc(CATEGORY),
      Sort.Order.desc(PRODUCT_FILE_ID)));

    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), newSort);
  }

  @Override
  public Criteria getCriteria(String organizationId,
                              String category,
                              String productCode,
                              String productFileId,
                              String eprelCode,
                              String gtinCode){

    Criteria criteria = Criteria.where(Product.Fields.organizationId).is(organizationId);

    if(category != null){
      criteria.and(Product.Fields.category).is(category);
    }
    if(productCode != null){
      criteria.and(Product.Fields.productCode).is(productCode);
    }
    if(productFileId != null){
      criteria.and(Product.Fields.productFileId).is(productFileId);
    }
    if(eprelCode != null){
      criteria.and(Product.Fields.eprelCode).regex(".*" + eprelCode + ".*", "i"); // Contains, case-insensitive
    }
    if(gtinCode != null){
      criteria.and(Product.Fields.gtinCode).regex(".*" + gtinCode + ".*", "i"); // Contains, case-insensitive
    }


    return criteria;
  }

  private Pageable getPageable(Pageable pageable) {
    if (pageable == null) {
      return PageRequest.of(0, 10, Sort.unsorted());
    }
    return pageable;
  }

  @Override
  public Long getCount(Criteria criteria){
    Query query = new Query();
    query.addCriteria(criteria);
    return mongoTemplate.count(query, Product.class);
  }

  @Override
  public List<Product> findDistinctProductFileIdAndCategoryByOrganizationId(String organizationId) {
    Aggregation aggregation = Aggregation.newAggregation(
      Aggregation.match(Criteria.where(ORGANIZATION_ID).is(organizationId)),
      Aggregation.group(PRODUCT_FILE_ID, CATEGORY),
      Aggregation.project()
        .and("_id." + PRODUCT_FILE_ID).as(PRODUCT_FILE_ID)
        .and("_id." + CATEGORY).as(CATEGORY)
    );

    AggregationResults<Product> results = mongoTemplate.aggregate(
      aggregation, PRODUCT, Product.class
    );

    return results.getMappedResults();
  }

}

