package com.st3.uber.dto.user;

public record BlockStatusDto(
        boolean blocked,
        String reason
) {}

