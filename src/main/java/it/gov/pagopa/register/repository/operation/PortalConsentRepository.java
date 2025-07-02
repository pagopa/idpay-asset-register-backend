package it.gov.pagopa.register.repository.operation;

import it.gov.pagopa.register.model.role.PortalConsent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PortalConsentRepository extends MongoRepository<PortalConsent, String> {
}
