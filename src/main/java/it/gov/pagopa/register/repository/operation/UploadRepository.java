package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.model.operation.UploadCsv;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;


public interface UploadRepository extends MongoRepository<UploadCsv, String> {

    Optional<UploadCsv> findByIdUpload(String idUpload);
}
