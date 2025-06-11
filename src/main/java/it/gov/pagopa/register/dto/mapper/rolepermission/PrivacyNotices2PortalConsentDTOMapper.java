package it.gov.pagopa.register.dto.mapper.rolepermission;

import it.gov.pagopa.register.dto.onetrust.PrivacyNoticesDTO;
import it.gov.pagopa.register.dto.rolepermission.PortalConsentDTO;
import org.springframework.stereotype.Service;

@Service
public class PrivacyNotices2PortalConsentDTOMapper {

    public PortalConsentDTO apply(PrivacyNoticesDTO privacyNotices, boolean firstAcceptance) {
        return PortalConsentDTO.builder()
                .versionId(privacyNotices.getVersion().getId())
                .firstAcceptance(firstAcceptance)
                .build();
    }
}
