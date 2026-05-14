package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.model.operation.ProducersInitiative;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.model.operation.ProductFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ProductRepository extends MongoRepository<Product, String>, ProductSpecificRepository {


  Optional<Product> findByGtinCodeAndInitiativeId(String gtinCode, String initiativeId);

  List<Product> findByGtinCodeInAndInitiativeId(List<String> gtinCodes, String initiativeId);
}

