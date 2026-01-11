package com.st3.uber.exception;

public class TokenAlreadyUsedException extends TokenException {
  public TokenAlreadyUsedException(String message) {
    super(message);
  }
}
