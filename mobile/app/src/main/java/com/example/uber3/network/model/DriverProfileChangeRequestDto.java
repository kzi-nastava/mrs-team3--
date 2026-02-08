package com.example.uber3.network.model;

import com.example.uber3.network.enums.VehicleType;

public class DriverProfileChangeRequestDto {

    public String firstName;
    public String lastName;
    public String phoneNumber;
    public String address;
    public String profileImage;

    public String vehicleModel;
    public String vehicleRegistrationNumber;
    public Integer vehicleSeatingCapacity;
    public VehicleType vehicleType;

    public Boolean babyTransport;
    public Boolean petTransport;

    public DriverProfileChangeRequestDto(
            String firstName,
            String lastName,
            String phoneNumber,
            String address,
            String profileImage,
            String vehicleModel,
            String vehicleRegistrationNumber,
            Integer vehicleSeatingCapacity,
            VehicleType vehicleType,
            Boolean babyTransport,
            Boolean petTransport
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.profileImage = profileImage;
        this.vehicleModel = vehicleModel;
        this.vehicleRegistrationNumber = vehicleRegistrationNumber;
        this.vehicleSeatingCapacity = vehicleSeatingCapacity;
        this.vehicleType = vehicleType;
        this.babyTransport = babyTransport;
        this.petTransport = petTransport;
    }
}
