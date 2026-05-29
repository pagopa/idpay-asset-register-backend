package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.model.operation.ProducersInitiative;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProducersInitiativeRepository
  extends MongoRepository<ProducersInitiative, String> {

  List<ProducersInitiative> findByProducerIdOrderByInitiativeNameAsc(String producerId);

  Page<ProducersInitiative> findByInitiativeId(String initiativeId, Pageable pageable);

  boolean existsByProducerIdAndInitiativeIdAndEnabledTrue(String producerId, String initiativeId);
}
