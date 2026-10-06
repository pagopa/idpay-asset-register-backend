package it.gov.pagopa.register.exception.operation;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.register.constants.ExceptionConstants.ExceptionCode.INVALID_REQUEST;

public class InvalidProducerImportException extends ServiceException {

  public InvalidProducerImportException(String message, Throwable cause) {
    super(INVALID_REQUEST, message, null, false, cause);
  }
}
