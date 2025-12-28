package com.st3.uber.dto.auth;

import lombok.Data;

@Data
public class RegisterPassengerRequest{
        String email;
        String password;
        String name;
        String surname;
        String phoneNumber;
        String address;
        String base64Image;
        String extension;
}

