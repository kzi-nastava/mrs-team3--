package com.example.uber3.network.model;

public class ProfileResponse {

    public Long id;
    public String email;
    public String firstName;
    public String lastName;
    public String phoneNumber;
    public String address;
    public String profileImage;

    // DRIVER FIELDS
    public Vehicle vehicle;
    public Boolean active;
    public Integer activeMinutes24h;

    public static class Vehicle {
        public Long id;
        public String model;
        public String type;
        public String registrationNumber;
        public int seatingCapacity;
        public boolean babyTransport;
        public boolean petTransport;
    }
}
