package com.example.uber3.network.model.location;

public class LocationRequest {

    public double latitude;
    public double longitude;
    public String address;

    public LocationRequest(
            double lat,
            double lon,
            String address
    ) {
        this.latitude = lat;
        this.longitude = lon;
        this.address = address;
    }
}
