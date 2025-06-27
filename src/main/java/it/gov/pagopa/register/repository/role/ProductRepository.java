package it.gov.pagopa.register.repository.role;

import it.gov.pagopa.register.model.role.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;



@Repository
public interface ProductRepository extends MongoRepository<Product, String> {


  @Query("{ " +
    "'organizationId': ?0, " +
    "'category': ?1, " +
    "'productCode': ?2, " +
    "'productFileId': ?3, " +
    "'eprelCode': ?4, " +
    "'gtinCode': ?5 " +
    "}")
  Page<Product> findProducts(
    String organizationId,
    String category,
    String productCode,
    String productFileId,
    String eprelCode,
    String gtinCode,
    Pageable pageable
  );
}
