package com.st3.uber.dto.user.admin;

import jakarta.validation.constraints.Size;

public record AdminProfileChangeDecisionDto(
        boolean approved,

        @Size(max = 255, message = "Reject reason too long")
        String rejectReason
) {}
