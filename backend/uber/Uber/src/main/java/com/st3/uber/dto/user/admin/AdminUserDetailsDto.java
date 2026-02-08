package com.st3.uber.dto.user.admin;

import com.st3.uber.enums.UserRole;

public record AdminUserDetailsDto(
        Long id,
        String name,
        String surname,
        String email,
        String phoneNumber,
        String address,
        UserRole role,
        boolean blocked,
        String blockReason,
        boolean verified
) {}
