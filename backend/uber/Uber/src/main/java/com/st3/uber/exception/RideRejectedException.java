package com.st3.uber.exception;

import com.st3.uber.enums.RideRejectReason;

public class RideRejectedException extends RuntimeException {

    private final RideRejectReason reason;

    public RideRejectedException(RideRejectReason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public RideRejectReason getReason() {
        return reason;
    }
}
