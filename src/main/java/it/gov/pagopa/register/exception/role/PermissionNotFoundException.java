package it.gov.pagopa.register.exception.role;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.register.constants.ExceptionConstants;

public class PermissionNotFoundException extends ServiceException {

    public PermissionNotFoundException(String message) {
        this(ExceptionConstants.ExceptionCode.PERMISSIONS_NOT_FOUND, message);
    }

    public PermissionNotFoundException(String code, String message) {
        this(code, message,null, false, null);
    }

    public PermissionNotFoundException(String code, String message, ServiceExceptionPayload response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }

}
