package it.gov.pagopa.register.exception.role;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.register.constants.RoleConstants;

public class ConsentNotFoundException extends ServiceException {

  public ConsentNotFoundException(String message) {
    this(RoleConstants.ExceptionCode.CONSENT_NOT_FOUND, message);
  }

  public ConsentNotFoundException(String code, String message) {
    this(code, message, null, false, null);
  }

  public ConsentNotFoundException(String code, String message, ServiceExceptionPayload response, boolean printStackTrace, Throwable ex) {
    super(code, message, response, printStackTrace, ex);
  }
}
