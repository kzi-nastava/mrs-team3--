package com.example.uber3.network.model.driver;


public class UpdateLocationRequest {
    public double latitude;
    public double longitude;

    public UpdateLocationRequest(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}