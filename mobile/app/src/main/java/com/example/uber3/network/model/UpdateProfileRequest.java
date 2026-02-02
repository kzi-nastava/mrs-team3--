package com.example.uber3.network.model;

public class UpdateProfileRequest {

    public String firstName;
    public String lastName;
    public String phoneNumber;
    public String address;

    public UpdateProfileRequest(String firstName, String lastName,
                                String phoneNumber, String address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }
}
