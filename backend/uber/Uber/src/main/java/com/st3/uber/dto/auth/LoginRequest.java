package com.st3.uber.dto.auth;

public record LoginRequest(
        String email,
        String password
) {}
