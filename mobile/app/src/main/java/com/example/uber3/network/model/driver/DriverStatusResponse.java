package com.example.uber3.network.model.driver;

public class DriverStatusResponse {

    private boolean active;
    private boolean activityRequest;
    private boolean inRide;

    public boolean isActive() {
        return active;
    }

    public boolean isActivityRequest() {
        return activityRequest;
    }

    public boolean isInRide() {
        return inRide;
    }
}
