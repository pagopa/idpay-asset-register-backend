package it.gov.pagopa.register.repository.rolepermission;

import it.gov.pagopa.register.model.rolepermission.PortalConsent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PortalConsentRepository extends MongoRepository<PortalConsent, String> {
}
