package com.st3.uber.util;

import java.util.UUID;

public class GenerateVerificationToken {

    private GenerateVerificationToken() {}

    public static String generateToken() {
        return UUID.randomUUID().toString();
    }
}
