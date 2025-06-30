package it.gov.pagopa.register.exception.operation;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.register.constants.ReportConstants;

public class ReportNotFoundException extends ServiceException {
    public ReportNotFoundException(String message) {
        this(ReportConstants.ExceptionCode.REPORT_NOT_FOUND, message);
    }

    public ReportNotFoundException(String code, String message) {
        this(code, message, null, false, null);
    }

    public ReportNotFoundException(String code, String message, ServiceExceptionPayload response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }

}
