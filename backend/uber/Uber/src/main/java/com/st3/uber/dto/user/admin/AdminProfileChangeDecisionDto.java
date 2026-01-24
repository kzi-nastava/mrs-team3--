package com.st3.uber.dto.user.admin;

public record AdminProfileChangeDecisionDto(
        boolean approved,
        String rejectReason
) {}
