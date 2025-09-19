package it.gov.pagopa.register.service.role;

import it.gov.pagopa.register.dto.role.PortalConsentDTO;

public interface PortalConsentService {

    PortalConsentDTO get(String userId);

    void save(String userId, PortalConsentDTO consent);
    void remove(String userId);
}
