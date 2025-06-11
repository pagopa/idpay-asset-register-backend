package it.gov.pagopa.register.service.rolepermission;

import it.gov.pagopa.register.dto.rolepermission.PortalConsentDTO;

public interface PortalConsentService {

    PortalConsentDTO get(String userId);

    void save(String userId, PortalConsentDTO consent);
}
