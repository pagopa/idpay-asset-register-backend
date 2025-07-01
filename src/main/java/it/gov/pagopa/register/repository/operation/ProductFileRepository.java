package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.model.operation.ProductFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProductFileRepository extends MongoRepository<ProductFile, String> {
  Optional<ProductFile> findByUploadId(String uploadId);
  Optional<ProductFile> findByOrganizationIdAndFileName(String organizationId, String fileName);

  Page<ProductFile> findByOrganizationIdAndUploadStatusNot(String organizationId, String uploadStatus, Pageable pageable);
}
