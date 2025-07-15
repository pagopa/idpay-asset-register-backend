package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.model.operation.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductRepository extends MongoRepository<Product, String>, ProductSpecificRepository {

}

