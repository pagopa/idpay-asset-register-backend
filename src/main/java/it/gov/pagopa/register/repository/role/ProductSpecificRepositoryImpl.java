package it.gov.pagopa.register.repository.role;

import it.gov.pagopa.register.model.role.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

public class ProductSpecificRepositoryImpl implements ProductSpecificRepository{

  private final MongoTemplate mongoTemplate;

  public ProductSpecificRepositoryImpl(MongoTemplate mongoTemplate){
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public List<Product> findByFilter (Criteria criteria, Pageable pageable) {
    return mongoTemplate.find(
      Query.query(criteria)
        .with(this.getPageable(pageable)),
      Product.class);
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
      criteria.and(Product.Fields.eprelCode).is(eprelCode);
    }
    if(gtinCode != null){
      criteria.and(Product.Fields.gtinCode).is(gtinCode);
    }

    return criteria;
  }

  private Pageable getPageable(Pageable pageable) {
    if (pageable == null) {
      return PageRequest.of(0, 15, Sort.by("registrationDate"));
    }
    return pageable;
  }

  @Override
  public Long getCount(Criteria criteria){
    Query query = new Query();
    query.addCriteria(criteria);
    return mongoTemplate.count(query, Product.class);
  }
}
