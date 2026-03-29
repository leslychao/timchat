package ru.timchat.common.error;

import lombok.Getter;

@Getter
public abstract class ApiException extends RuntimeException {

  private final String errorCode;
  private final String messageKey;
  private final Object[] args;

  protected ApiException(String errorCode, String messageKey, Object... args) {
    super(messageKey);
    this.errorCode = errorCode;
    this.messageKey = messageKey;
    this.args = args;
  }

  protected ApiException(
      String errorCode, String messageKey, Throwable cause, Object... args) {
    super(messageKey, cause);
    this.errorCode = errorCode;
    this.messageKey = messageKey;
    this.args = args;
  }
}
