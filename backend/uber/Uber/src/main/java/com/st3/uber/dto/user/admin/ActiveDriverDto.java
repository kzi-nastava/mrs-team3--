package com.st3.uber.dto.user.admin;

public record ActiveDriverDto(
        Long id,
        String name,
        String surname,
        String email,
        boolean blocked
) {}
