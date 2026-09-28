package it.gov.pagopa.register.exception.operation;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import org.springframework.http.HttpStatus;

import static it.gov.pagopa.register.constants.ExceptionConstants.ExceptionCode.INVALID_REQUEST;

public class InvalidProducerImportException extends ClientExceptionWithBody {

  public InvalidProducerImportException(String message, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, INVALID_REQUEST, message, cause);
  }
}
