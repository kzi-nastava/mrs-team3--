package com.example.uber3.network.model.vehicle;

public class VehicleRequest {
    private String model;
    private String type; // "STANDARD" | "VAN" | "LUXURY"
    private String registrationNumber;
    private int seatingCapacity;
    private boolean babyTransport;
    private boolean petTransport;

    public VehicleRequest() {}

    public VehicleRequest(String model, String type, String registrationNumber,
                          int seatingCapacity, boolean babyTransport, boolean petTransport) {
        this.model = model;
        this.type = type;
        this.registrationNumber = registrationNumber;
        this.seatingCapacity = seatingCapacity;
        this.babyTransport = babyTransport;
        this.petTransport = petTransport;
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public int getSeatingCapacity() { return seatingCapacity; }
    public void setSeatingCapacity(int seatingCapacity) { this.seatingCapacity = seatingCapacity; }

    public boolean isBabyTransport() { return babyTransport; }
    public void setBabyTransport(boolean babyTransport) { this.babyTransport = babyTransport; }

    public boolean isPetTransport() { return petTransport; }
    public void setPetTransport(boolean petTransport) { this.petTransport = petTransport; }
}
