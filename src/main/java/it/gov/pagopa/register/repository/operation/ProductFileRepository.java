package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.model.operation.ProductFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProductFileRepository extends MongoRepository<ProductFile, String> {
  Optional<ProductFile> findByIdUpload(String idUpload);

  Page<ProductFile> findByIdOrgAndStatusNot(String idOrg, String status, Pageable pageable);
}
