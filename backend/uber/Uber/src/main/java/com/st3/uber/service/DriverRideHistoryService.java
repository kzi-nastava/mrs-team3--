package com.st3.uber.service;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Ride;
import com.st3.uber.dto.location.LocationRequest;
import com.st3.uber.dto.ride.ReportInconsistencyResponse;
import com.st3.uber.dto.ride.SubmitRatingRequest;
import com.st3.uber.dto.user.driver.DriverRideHistoryResponse;
import com.st3.uber.dto.user.driver.DriverRideDetailResponse;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.RideRepository;
import com.st3.uber.repository.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriverRideHistoryService {

    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;

    // Only these statuses should appear in history
    private static final List<RideStatus> TERMINAL_STATUSES = Arrays.asList(
            RideStatus.COMPLETED,
            RideStatus.FINISHED_EARLY,
            RideStatus.CANCELLED,
            RideStatus.CANCELLED_BY_DRIVER,
            RideStatus.CANCELLED_BY_PASSENGER
    );

    public DriverRideHistoryService(RideRepository rideRepository, DriverRepository driverRepository) {
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional(readOnly = true)
    public List<DriverRideHistoryResponse> getDriverRideHistory(
            Long driverId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        List<Ride> rides;

        if (startDate != null && endDate != null) {
            rides = rideRepository.findByDriverAndStartedAtBetween(driver, startDate, endDate);
        } else if (startDate != null) {
            rides = rideRepository.findByDriverAndStartedAtAfter(driver, startDate);
        } else if (endDate != null) {
            rides = rideRepository.findByDriverAndStartedAtBefore(driver, endDate);
        } else {
            rides = rideRepository.findByDriver(driver);
        }

        // Filter to only include terminal statuses (completed, cancelled, finished early)
        return rides.stream()
                .filter(ride -> TERMINAL_STATUSES.contains(ride.getStatus()))
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DriverRideDetailResponse getDriverRideDetail(Long driverId, Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driverId)) {
            throw new RuntimeException("Driver was not assigned to this ride");
        }

        // Verify this is a terminal status ride
        if (!TERMINAL_STATUSES.contains(ride.getStatus())) {
            throw new RuntimeException("Ride is not yet completed");
        }

        return mapToDetailResponse(ride);
    }

    private DriverRideHistoryResponse mapToHistoryResponse(Ride ride) {
        return new DriverRideHistoryResponse(
                ride.getId(),
                ride.getStartLocation().getAddress(),
                ride.getEndLocation().getAddress(),
                ride.getStartedAt(),
                ride.getFinishedAt(),
                ride.getStatus(),
                ride.getCalculatedPrice(),
                ride.getDistance(),
                ride.getCancelledBy() != null,
                ride.getCancelledBy(),
                ride.isPanic(),
                ride.getPassengers().stream()
                        .map(p -> p.getName() + " " + p.getSurname())
                        .collect(Collectors.toList())
        );
    }

    private DriverRideDetailResponse mapToDetailResponse(Ride ride) {
        // Map reviews to ReviewResponse
        List<SubmitRatingRequest> reviews = ride.getReviews().stream()
                .map(review -> new SubmitRatingRequest(
                        review.getDriverRating(),
                        review.getVehicleRating(),
                        review.getReviewComment()
                ))
                .collect(Collectors.toList());

        // Get registered passengers
        List<String> registeredPassengers = ride.getPassengers().stream()
                .map(p -> p.getName() + " " + p.getSurname())
                .collect(Collectors.toList());

        // Get invited passengers (email from RideInvite)
        List<String> invitedPassengers = ride.getInvites().stream()
                .map(invite -> invite.getEmail())
                .collect(Collectors.toList());

        // Map inconsistency reports
        List<ReportInconsistencyResponse> reports = ride.getInconsistencyReports().stream()
                .map(report -> new ReportInconsistencyResponse(
                        report.getId(),
                        ride.getId(),
                        report.getReportText(),
                        report.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new DriverRideDetailResponse(
                ride.getId(),
                ride.getStartLocation().getAddress(),
                ride.getStartLocation().getLat(),
                ride.getStartLocation().getLng(),
                ride.getEndLocation().getAddress(),
                ride.getEndLocation().getLat(),
                ride.getEndLocation().getLng(),
                ride.getStartedAt(),
                ride.getFinishedAt(),
                ride.getStatus(),
                ride.getCalculatedPrice(),
                ride.getDistance(),
                ride.getVehicleType(),
                ride.getCancelledBy() != null,
                ride.getCancelledBy(),
                ride.getTerminationReason(),
                ride.isPanic(),
                ride.isStoppedEarly(),
                registeredPassengers,
                invitedPassengers,
                ride.getRideStops().stream()
                        .map(loc -> new LocationRequest(loc.getLat(), loc.getLng(), loc.getAddress()))
                        .collect(Collectors.toList()),
                ride.getActualRideStops().stream()
                        .map(loc -> new LocationRequest(loc.getLat(), loc.getLng(), loc.getAddress()))
                        .collect(Collectors.toList()),
                reviews,
                reports
        );
    }
}