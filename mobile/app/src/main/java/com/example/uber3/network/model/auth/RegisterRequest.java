package com.example.uber3.network.model.auth;

public class RegisterRequest {
    private String email;
    private String password;
    private String name;
    private String surname;
    private String phoneNumber;
    private String address;

    private String base64Image;
    private String extension;

    public RegisterRequest(
            String email,
            String password,
            String name,
            String surname,
            String phoneNumber,
            String address,
            String base64Image,
            String extension
    ) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.surname = surname;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.base64Image = base64Image;
        this.extension = extension;
    }
}
