package ru.timchat.common.error;

public class ValidationException extends ApiException {

  public ValidationException(String messageKey, Object... args) {
    super("VALIDATION_ERROR", messageKey, args);
  }
}
