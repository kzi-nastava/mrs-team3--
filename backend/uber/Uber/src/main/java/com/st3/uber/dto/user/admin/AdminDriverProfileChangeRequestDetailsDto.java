package com.st3.uber.dto.user.admin;

import com.st3.uber.enums.VehicleType;

import java.time.LocalDateTime;

public record AdminDriverProfileChangeRequestDetailsDto(

        Long requestId,
        String status,
        LocalDateTime requestedAt,

        Long driverId,
        String driverEmail,
        String driverFirstName,
        String driverLastName,

        String oldFirstName,
        String oldLastName,
        String oldPhoneNumber,
        String oldAddress,

        String newFirstName,
        String newLastName,
        String newPhoneNumber,
        String newAddress,

        String oldVehicleModel,
        String oldVehicleRegistrationNumber,
        Integer oldVehicleSeatingCapacity,
        VehicleType oldVehicleType,
        Boolean oldBabyTransport,
        Boolean oldPetTransport,

        String newVehicleModel,
        String newVehicleRegistrationNumber,
        Integer newVehicleSeatingCapacity,
        VehicleType newVehicleType,
        Boolean newBabyTransport,
        Boolean newPetTransport

) {}
