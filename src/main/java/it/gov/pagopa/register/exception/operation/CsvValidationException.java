package it.gov.pagopa.register.exception.operation;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.register.constants.RoleConstants;

public class CsvValidationException extends ServiceException {

    public CsvValidationException(String message) {
        this(RoleConstants.ExceptionCode.INVALID_REQUEST, message);
    }

    public CsvValidationException(String code, String message) {
        this(code, message,null, false, null);
    }

    public CsvValidationException(String code, String message, ServiceExceptionPayload response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }

}
