package com.st3.uber.dto.user.driver;


import com.st3.uber.enums.VehicleType;

public record DriverProfileChangeRequestDto(

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

) {}