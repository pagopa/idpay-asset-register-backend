package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.model.operation.UploadCsv;
import org.springframework.data.mongodb.repository.MongoRepository;
//import test.registro.model.Prodotto;


public interface UploadRepository extends MongoRepository<UploadCsv, String> {

}
