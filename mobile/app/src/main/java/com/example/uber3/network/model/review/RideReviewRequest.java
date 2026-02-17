package com.example.uber3.network.model.review;

public class RideReviewRequest {
    public int driverRating;
    public int vehicleRating;
    public String comment;

    public RideReviewRequest() {
    }

    public RideReviewRequest(int driverRating, int vehicleRating, String comment) {
        this.driverRating = driverRating;
        this.vehicleRating = vehicleRating;
        this.comment = comment;
    }
}