package com.example.uber3.network.model.review;

public class RideReviewResponse {
    public Long rideId;
    public int driverRating;
    public int vehicleRating;
    public String comment;
    public String reviewedAt;
    public String message;

    public RideReviewResponse() {
    }

    public RideReviewResponse(Long rideId, int driverRating, int vehicleRating,
                              String comment, String reviewedAt, String message) {
        this.rideId = rideId;
        this.driverRating = driverRating;
        this.vehicleRating = vehicleRating;
        this.comment = comment;
        this.reviewedAt = reviewedAt;
        this.message = message;
    }
}