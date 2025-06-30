package it.gov.pagopa.register.repository.role;

import it.gov.pagopa.register.model.role.Product;
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

  private Pageable getPageable(Pageable pageable) {
    if (pageable == null) {
      return PageRequest.of(0, 15, Sort.by("registrationDate"));
    }
    return pageable;
  }
}
