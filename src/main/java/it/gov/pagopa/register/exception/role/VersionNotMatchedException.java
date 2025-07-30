package it.gov.pagopa.register.exception.role;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.register.constants.ExceptionConstants;

public class VersionNotMatchedException extends ServiceException {

    public VersionNotMatchedException(String message) {
        this(ExceptionConstants.ExceptionCode.VERSION_NOT_MATCHED, message);
    }

    public VersionNotMatchedException(String code, String message) {
        this(code, message,null, false, null);
    }

    public VersionNotMatchedException(String code, String message, ServiceExceptionPayload response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }

}
