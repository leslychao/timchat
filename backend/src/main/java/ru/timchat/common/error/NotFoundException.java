package ru.timchat.common.error;

public class NotFoundException extends ApiException {

  public NotFoundException(String messageKey, Object... args) {
    super("NOT_FOUND", messageKey, args);
  }
}
