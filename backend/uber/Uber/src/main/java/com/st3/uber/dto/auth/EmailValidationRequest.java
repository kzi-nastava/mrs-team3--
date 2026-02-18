package com.st3.uber.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailValidationRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email
) {}