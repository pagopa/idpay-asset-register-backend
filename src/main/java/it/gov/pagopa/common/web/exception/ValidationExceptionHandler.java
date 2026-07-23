package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler {

  private final ErrorDTO templateValidationErrorDTO;

  public ValidationExceptionHandler(@Nullable ErrorDTO templateValidationErrorDTO) {
    this.templateValidationErrorDTO = Optional.ofNullable(templateValidationErrorDTO)
      .orElse(new ErrorDTO("INVALID_REQUEST", "Invalid request"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorDTO handleValidationExceptions(
    MethodArgumentNotValidException ex, HttpServletRequest request) {

    String message = ex.getBindingResult().getAllErrors().stream()
      .map(error -> {
        String fieldName = ((FieldError) error).getField();
        String errorMessage = error.getDefaultMessage();
        return String.format("[%s]: %s", fieldName, errorMessage);
      }).collect(Collectors.joining("; "));

    log.info("A MethodArgumentNotValidException occurred handling request {}: HttpStatus 400 - {}",
      ErrorManager.getRequestDetails(request), message);
    log.debug("Something went wrong while validating http request", ex);

    return new ErrorDTO(templateValidationErrorDTO.getCode(), message);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorDTO handleMissingRequestHeaderExceptions(
    MissingRequestHeaderException ex, HttpServletRequest request) {

    String message = ex.getMessage();

    log.info("A MissingRequestHeaderException occurred handling request {}: HttpStatus 400 - {}",
      ErrorManager.getRequestDetails(request), message);
    log.debug("Something went wrong handling request", ex);

    return new ErrorDTO(templateValidationErrorDTO.getCode(), message);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorDTO handleConstraintViolationException(
    ConstraintViolationException ex, HttpServletRequest request) {

    String message = ex.getConstraintViolations().stream()
      .map(cv -> {
        String[] pathParts = cv.getPropertyPath().toString().split("\\.");
        String fieldName = pathParts.length > 0 ? pathParts[pathParts.length - 1] : cv.getPropertyPath().toString();

        String errorMessage = cv.getMessage();
        return String.format("[%s]: %s", fieldName, errorMessage);
      })
      .collect(Collectors.joining("; "));

    log.info("A ConstraintViolationException occurred handling request {}: HttpStatus 400 - {}",
      ErrorManager.getRequestDetails(request), message);
    log.debug("Something went wrong while validating http request parameters", ex);

    return new ErrorDTO(templateValidationErrorDTO.getCode(), message);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorDTO handleMethodArgumentTypeMismatchException(
    MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

    String message = buildTypeMismatchMessage(ex);

    log.info("A MethodArgumentTypeMismatchException occurred handling request {}: HttpStatus 400 - {}",
      ErrorManager.getRequestDetails(request), message);
    log.debug("Something went wrong while binding request parameters", ex);

    return new ErrorDTO(templateValidationErrorDTO.getCode(), message);
  }

  private String buildTypeMismatchMessage(MethodArgumentTypeMismatchException ex) {
    if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
      String allowedValues = Arrays.stream(ex.getRequiredType().getEnumConstants())
        .map(String::valueOf)
        .collect(Collectors.joining(", "));
      String invalidValue = String.valueOf(ex.getValue());
      return String.format("[%s]: invalid value '%s'. Allowed values: %s",
        ex.getName(), invalidValue, allowedValues);
    }

    String value = ex.getValue() != null ? ex.getValue().toString() : null;
    String valueMessage = StringUtils.hasText(value)
      ? String.format(" value '%s'", ex.getValue())
      : "";
    return String.format("[%s]: invalid request parameter%s", ex.getName(), valueMessage);
  }
}
