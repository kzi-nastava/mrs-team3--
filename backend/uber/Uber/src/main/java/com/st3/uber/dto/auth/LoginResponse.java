package com.st3.uber.dto.auth;

public record LoginResponse(
        Long id,
        String email,
        String role
) {}
