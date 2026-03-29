package ru.timchat.common.error;

public class ForbiddenException extends ApiException {

  public ForbiddenException(String messageKey, Object... args) {
    super("FORBIDDEN", messageKey, args);
  }
}
