package com.st3.uber.service;

import com.st3.uber.domain.*;
import com.st3.uber.dto.ride.RideReviewDetailResponse;
import com.st3.uber.dto.ride.SubmitRatingRequest;
import com.st3.uber.dto.ride.SubmitRatingResponse;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.PassengerRepository;
import com.st3.uber.repository.ReviewRepository;
import com.st3.uber.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private static final int REVIEW_DEADLINE_DAYS = 3;

    private final ReviewRepository reviewRepository;
    private final RideRepository rideRepository;
    private final PassengerRepository passengerRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         RideRepository rideRepository,
                         PassengerRepository passengerRepository) {
        this.reviewRepository = reviewRepository;
        this.rideRepository = rideRepository;
        this.passengerRepository = passengerRepository;
    }

    /**
     * Get ride details for the review page
     * Validates passenger access and review eligibility
     */
    @Transactional(readOnly = true)
    public RideReviewDetailResponse getRideForReview(Long rideId, Long passengerId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found with ID: " + rideId));

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        boolean isPassengerInRide = ride.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(passengerId));

        boolean isCreator = ride.getCreator().getId().equals(passengerId);

        if (!isPassengerInRide && !isCreator) {
            throw new IllegalArgumentException("You were not a passenger on this ride");
        }

        if (ride.getStatus() != RideStatus.COMPLETED && ride.getStatus() != RideStatus.FINISHED_EARLY) {
            throw new IllegalArgumentException("This ride is not completed yet");
        }

        if (ride.getFinishedAt() == null) {
            throw new IllegalArgumentException("Ride completion time is not available");
        }

        LocalDateTime deadline = ride.getFinishedAt().plusDays(REVIEW_DEADLINE_DAYS);
        boolean canReview = LocalDateTime.now().isBefore(deadline);

        Review existingReview = reviewRepository
                .findByRideIdAndPassengerId(rideId, passengerId)
                .orElse(null);

        return new RideReviewDetailResponse(
                ride.getId(),
                mapLocation(ride.getStartLocation()),
                mapLocation(ride.getEndLocation()),
                ride.getRideStops().stream()
                        .map(this::mapLocation)
                        .collect(Collectors.toList()),
                ride.getDriver() != null ? ride.getDriver().getName() + " " + ride.getDriver().getSurname() : "Unknown",
                ride.getVehicleType().name(),
                ride.getStartedAt(),
                ride.getFinishedAt(),
                ride.getDistance(),
                ride.getEstimatedTimeMinutes(),
                ride.getCalculatedPrice(),
                canReview,
                deadline,
                existingReview != null ? mapReview(existingReview) : null
        );
    }

    /**
     * Submit a new review or update an existing one
     * Only allowed within 3 days of ride completion
     */
    @Transactional
    public SubmitRatingResponse submitOrUpdateReview(Long rideId, Long passengerId,
                                                     SubmitRatingRequest request) {
        // Fetch the ride
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found with ID: " + rideId));

        // Fetch the passenger
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        // Validate passenger was part of the ride
        boolean isPassengerInRide = ride.getPassengers().stream()
                .anyMatch(p -> p.getId().equals(passengerId));

        boolean isCreator = ride.getCreator().getId().equals(passengerId);

        if (!isPassengerInRide && !isCreator) {
            throw new IllegalArgumentException("You were not a passenger on this ride");
        }

        // Validate ride is completed
        if (ride.getStatus() != RideStatus.COMPLETED && ride.getStatus() != RideStatus.FINISHED_EARLY) {
            throw new IllegalArgumentException("Cannot review a ride that is not completed");
        }

        // Validate review deadline
        if (ride.getFinishedAt() == null) {
            throw new IllegalArgumentException("Ride completion time is not available");
        }

        LocalDateTime deadline = ride.getFinishedAt().plusDays(REVIEW_DEADLINE_DAYS);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new IllegalArgumentException(
                    "Review deadline has passed. You can only review within " +
                            REVIEW_DEADLINE_DAYS + " days of ride completion"
            );
        }

        // Validate ratings
        validateRating(request.driverRating(), "Driver rating");
        validateRating(request.vehicleRating(), "Vehicle rating");

        // Check if review already exists
        Review review = reviewRepository
                .findByRideIdAndPassengerId(rideId, passengerId)
                .orElse(null);

        boolean isUpdate = review != null;
        String message;

        if (isUpdate) {
            // Update existing review
            review.setDriverRating(request.driverRating());
            review.setVehicleRating(request.vehicleRating());
            review.setReviewComment(request.comment());
            review.setReviewedAt(LocalDateTime.now());
            message = "Review updated successfully";
        } else {
            // Create new review
            review = new Review();
            review.setRide(ride);
            review.setPassenger(passenger);
            review.setDriverRating(request.driverRating());
            review.setVehicleRating(request.vehicleRating());
            review.setReviewComment(request.comment());
            review.setReviewedAt(LocalDateTime.now());
            message = "Review submitted successfully";
        }

        review = reviewRepository.save(review);

        return new SubmitRatingResponse(
                ride.getId(),
                review.getDriverRating(),
                review.getVehicleRating(),
                review.getReviewComment(),
                review.getReviewedAt(),
                message
        );
    }

    private void validateRating(Integer rating, String fieldName) {
        if (rating == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and 5");
        }
    }

    private RideReviewDetailResponse.LocationDto mapLocation(Location location) {
        return new RideReviewDetailResponse.LocationDto(
                location.getLat(),
                location.getLng(),
                location.getAddress()
        );
    }

    private RideReviewDetailResponse.ExistingReviewDto mapReview(Review review) {
        return new RideReviewDetailResponse.ExistingReviewDto(
                review.getDriverRating(),
                review.getVehicleRating(),
                review.getReviewComment(),
                review.getReviewedAt()
        );
    }
}