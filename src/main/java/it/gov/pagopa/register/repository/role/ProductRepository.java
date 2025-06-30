package it.gov.pagopa.register.repository.role;

import it.gov.pagopa.register.model.role.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface ProductRepository extends MongoRepository<Product, String>, ProductSpecificRepository {



}
