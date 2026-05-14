package it.gov.pagopa.register.repository.initiative;

import it.gov.pagopa.register.configuration.initiative.model.InitiativeConfig;
import it.gov.pagopa.register.model.operation.Product;
import it.gov.pagopa.register.repository.operation.ProductSpecificRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface InitiativeRepository extends MongoRepository<InitiativeConfig, String> {

}

