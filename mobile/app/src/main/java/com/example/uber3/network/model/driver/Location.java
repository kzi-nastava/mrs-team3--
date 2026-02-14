package com.example.uber3.network.model.driver;

public class Location {
    public double lat;
    public double lng;
    public String address;

    public Location() {}

    public Location(double lat, double lng, String address) {
        this.lat = lat;
        this.lng = lng;
        this.address = address;
    }
}