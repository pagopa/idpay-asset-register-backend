package it.gov.pagopa.register.repository.initiative;

import it.gov.pagopa.register.model.initiative.InitiativeConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface InitiativeRepository extends MongoRepository<InitiativeConfig, String> {

}

