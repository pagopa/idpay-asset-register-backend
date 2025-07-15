package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.model.operation.ProductFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductFileRepository extends MongoRepository<ProductFile, String> {

  Page<ProductFile> findByOrganizationIdAndUploadStatusNot(String organizationId, String uploadStatus, Pageable pageable);

  Optional<ProductFile> findByIdAndOrganizationId(String id, String organizationId);

  boolean existsByOrganizationIdAndUploadStatusIn(String organizationId, List<String> uploadStatuses);

}
