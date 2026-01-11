package com.st3.uber.exception;

public abstract class TokenException extends RuntimeException{
    public TokenException(String message) {
        super(message);
    }
}

