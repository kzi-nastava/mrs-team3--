package com.st3.uber.exception;

public class TokenExpiredException extends TokenException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
