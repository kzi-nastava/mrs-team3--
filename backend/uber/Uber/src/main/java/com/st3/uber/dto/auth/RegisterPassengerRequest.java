package com.st3.uber.dto.auth;

public record RegisterPassengerRequest(
        String email,
        String password,
        String firstName,
        String lastName,
        String phoneNumber,
        String address
) {}