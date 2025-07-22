package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.model.operation.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.register.constants.AssetRegisterConstants.*;

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

    boolean sortByEnergyClass = hasSortProperty(sort, ENERGY_CLASS);
    boolean sortByCategory = hasSortProperty(sort, CATEGORY);
    boolean sortByBatchName = hasSortProperty(sort, BATCH_NAME);

    if (sortByEnergyClass) {
      return aggregateResults(energyClassAggregation(criteria, pageable));
    }

    if (sortByCategory || sortByBatchName) {
      return aggregateResults(categoryAggregation(criteria, pageable, sortByBatchName));
    }

    return mongoTemplate.find(Query.query(criteria).with(pageable), Product.class);
  }

  private boolean hasSortProperty(Sort sort, String property) {
    return sort.stream().anyMatch(order -> order.getProperty().equalsIgnoreCase(property));
  }

  private List<Product> aggregateResults(Aggregation aggregation) {
    return mongoTemplate.aggregate(aggregation, PRODUCT, Product.class).getMappedResults();
  }

  private Aggregation energyClassAggregation(Criteria criteria, Pageable pageable) {
    Sort.Direction direction = getSortDirection(pageable, ENERGY_CLASS);

    return Aggregation.newAggregation(
      Aggregation.addFields()
        .addField("energyRank")
        .withValue(
          ConditionalOperators.switchCases(
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("A+++")).then(10),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("A++")).then(9),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("A+")).then(8),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("A")).then(7),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("B")).then(6),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("C")).then(5),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("D")).then(4),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("E")).then(3),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(ENERGY_CLASS).equalToValue("F")).then(2)
          ).defaultTo(1)
        ).build(),
      Aggregation.match(criteria),
      Aggregation.sort(Sort.by(direction, "energyRank")),
      Aggregation.skip(pageable.getOffset()),
      Aggregation.limit(pageable.getPageSize())
    );
  }

  private Aggregation categoryAggregation(Criteria criteria, Pageable pageable, boolean sortByBatchName) {
    Sort.Direction direction = getSortDirection(pageable, CATEGORY);
    List<Sort.Order> orders = new ArrayList<>();
    orders.add(new Sort.Order(direction, "categoryIt"));

    if (sortByBatchName) {
      orders.add(new Sort.Order(direction, PRODUCT_FILE_ID));
    }

    return Aggregation.newAggregation(
      Aggregation.addFields()
        .addField("categoryIt")
        .withValue(
          ConditionalOperators.switchCases(
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(CATEGORY).equalToValue(WASHINGMACHINES)).then(WASHINGMACHINES_IT),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(CATEGORY).equalToValue(WASHERDRIERS)).then(WASHERDRIERS_IT),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(CATEGORY).equalToValue(OVENS)).then(OVENS_IT),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(CATEGORY).equalToValue(RANGEHOODS)).then(RANGEHOODS_IT),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(CATEGORY).equalToValue(DISHWASHERS)).then(DISHWASHERS_IT),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(CATEGORY).equalToValue(TUMBLEDRYERS)).then(TUMBLEDRYERS_IT),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(CATEGORY).equalToValue(REFRIGERATINGAPPL)).then(REFRIGERATINGAPPL_IT),
            ConditionalOperators.Switch.CaseOperator.when(ComparisonOperators.valueOf(CATEGORY).equalToValue(COOKINGHOBS)).then(COOKINGHOBS_IT)
          )
        ).build(),
      Aggregation.match(criteria),
      Aggregation.sort(Sort.by(orders)),
      Aggregation.skip(pageable.getOffset()),
      Aggregation.limit(pageable.getPageSize())
    );
  }
  private Sort.Direction getSortDirection(Pageable pageable, String property) {
    Sort.Order order = pageable.getSort().getOrderFor(property);
    return order != null ? order.getDirection() : Sort.Direction.ASC;
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

