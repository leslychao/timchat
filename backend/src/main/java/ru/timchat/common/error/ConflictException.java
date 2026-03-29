package ru.timchat.common.error;

public class ConflictException extends ApiException {

  public ConflictException(String messageKey, Object... args) {
    super("CONFLICT", messageKey, args);
  }
}
