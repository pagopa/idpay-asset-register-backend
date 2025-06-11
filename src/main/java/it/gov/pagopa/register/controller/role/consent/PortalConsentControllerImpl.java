package it.gov.pagopa.register.controller.role.consent;

import it.gov.pagopa.register.dto.role.PortalConsentDTO;
import it.gov.pagopa.register.service.role.PortalConsentService;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class PortalConsentControllerImpl implements PortalConsentController {

    private final PortalConsentService portalConsentService;

    public PortalConsentControllerImpl(PortalConsentService portalConsentService) {
        this.portalConsentService = portalConsentService;
    }

    @Override
    public PortalConsentDTO getPortalConsent(String userId) {
        return portalConsentService.get(userId);
    }

    @Override
    public void savePortalConsent(String userId, PortalConsentDTO consent) {
        portalConsentService.save(userId, consent);
    }
}
