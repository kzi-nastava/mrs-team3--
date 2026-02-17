package com.example.uber3.network.model.review;

public class RideReviewDetailResponse {
    public Long rideId;
    public String driverName;
    public String vehicleType;
    public String startTime;
    public String endTime;
    public double distance;
    public int duration;
    public double price;
    public boolean canReview;
    public String reviewDeadline;
    public ExistingReview existingReview;

    public static class ExistingReview {
        public int driverRating;
        public int vehicleRating;
        public String comment;
        public String reviewedAt;

        public ExistingReview() {
        }

        public ExistingReview(int driverRating, int vehicleRating, String comment, String reviewedAt) {
            this.driverRating = driverRating;
            this.vehicleRating = vehicleRating;
            this.comment = comment;
            this.reviewedAt = reviewedAt;
        }
    }

    public RideReviewDetailResponse() {
    }
}