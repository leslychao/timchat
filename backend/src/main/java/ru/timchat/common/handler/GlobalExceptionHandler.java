package ru.timchat.common.handler;

import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.timchat.common.error.ApiException;
import ru.timchat.common.error.ConflictException;
import ru.timchat.common.error.ErrorResponse;
import ru.timchat.common.error.ForbiddenException;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.common.error.ValidationException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse handleNotFound(NotFoundException ex, Locale locale) {
    return buildResponse(ex, locale);
  }

  @ExceptionHandler(ForbiddenException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ErrorResponse handleForbidden(ForbiddenException ex, Locale locale) {
    return buildResponse(ex, locale);
  }

  @ExceptionHandler(ConflictException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponse handleConflict(ConflictException ex, Locale locale) {
    return buildResponse(ex, locale);
  }

  @ExceptionHandler(ValidationException.class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  public ErrorResponse handleValidation(
      ValidationException ex, Locale locale) {
    return buildResponse(ex, locale);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  public ErrorResponse handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, Locale locale) {
    String traceId = generateTraceId();
    String details = ex.getBindingResult().getFieldErrors().stream()
        .map(this::formatFieldError)
        .collect(Collectors.joining("; "));
    String message = resolveMessage("error.validation", locale);

    log.warn("Validation error [traceId={}]: {}", traceId, details);

    return new ErrorResponse("VALIDATION_ERROR", message, details, traceId);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResponse handleGeneric(Exception ex, Locale locale) {
    String traceId = generateTraceId();
    String message = resolveMessage("error.internal", locale);

    log.error("Unhandled exception [traceId={}]", traceId, ex);

    return new ErrorResponse("INTERNAL_ERROR", message, null, traceId);
  }

  private ErrorResponse buildResponse(ApiException ex, Locale locale) {
    String traceId = generateTraceId();
    String message = resolveMessage(ex.getMessageKey(), locale, ex.getArgs());

    log.warn("API error [traceId={}, code={}]: {}",
        traceId, ex.getErrorCode(), message);

    return new ErrorResponse(ex.getErrorCode(), message, null, traceId);
  }

  private String resolveMessage(
      String key, Locale locale, Object... args) {
    try {
      return messageSource.getMessage(key, args, locale);
    } catch (NoSuchMessageException e) {
      return key;
    }
  }

  private String formatFieldError(FieldError error) {
    return error.getField() + ": " + error.getDefaultMessage();
  }

  private String generateTraceId() {
    return UUID.randomUUID().toString();
  }
}
