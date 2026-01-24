package com.st3.uber.exception;


public class PendingProfileChangeRequestException extends RuntimeException {

    public PendingProfileChangeRequestException(String message) {
        super(message);
    }
}