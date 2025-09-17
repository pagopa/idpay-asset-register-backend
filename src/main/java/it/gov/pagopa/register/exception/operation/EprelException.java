package it.gov.pagopa.register.exception.operation;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.register.constants.ExceptionConstants;

public class EprelException extends ServiceException {

  public EprelException(String message) {
    this(ExceptionConstants.ExceptionCode.EPREL_EXCEPTION, message);
  }

  public EprelException(String code, String message) {
    this(code, message, null, false, null);
  }

  public EprelException(String code, String message, ServiceExceptionPayload response, boolean printStackTrace, Throwable ex) {
    super(code, message, response, printStackTrace, ex);
  }
}
