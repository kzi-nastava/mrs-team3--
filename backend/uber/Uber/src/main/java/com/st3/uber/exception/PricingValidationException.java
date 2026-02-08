package com.st3.uber.exception;

public class PricingValidationException extends RuntimeException {

    public PricingValidationException(String message) {
        super(message);
    }

    public PricingValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}