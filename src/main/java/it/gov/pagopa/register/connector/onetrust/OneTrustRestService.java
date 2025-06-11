package it.gov.pagopa.register.connector.onetrust;

import it.gov.pagopa.register.dto.onetrust.PrivacyNoticesDTO;

import java.time.LocalDateTime;

public interface OneTrustRestService {

    PrivacyNoticesDTO getPrivacyNotices(String id);
    PrivacyNoticesDTO getPrivacyNotices(String id, LocalDateTime date);
}
