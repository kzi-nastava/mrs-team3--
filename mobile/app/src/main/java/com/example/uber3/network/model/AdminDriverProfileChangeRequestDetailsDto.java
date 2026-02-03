package com.example.uber3.network.model;

public class AdminDriverProfileChangeRequestDetailsDto {

    public Long requestId;
    public String status;
    public String requestedAt;

    public String driverEmail;
    public String driverFirstName;
    public String driverLastName;

    public String oldFirstName;
    public String newFirstName;

    public String oldLastName;
    public String newLastName;

    public String oldPhoneNumber;
    public String newPhoneNumber;

    public String oldAddress;
    public String newAddress;

    public String oldVehicleModel;
    public String newVehicleModel;

    public String oldVehicleRegistrationNumber;
    public String newVehicleRegistrationNumber;

    public Integer oldVehicleSeatingCapacity;
    public Integer newVehicleSeatingCapacity;

    public String oldVehicleType;
    public String newVehicleType;

    public Boolean oldBabyTransport;
    public Boolean newBabyTransport;

    public Boolean oldPetTransport;
    public Boolean newPetTransport;
}
