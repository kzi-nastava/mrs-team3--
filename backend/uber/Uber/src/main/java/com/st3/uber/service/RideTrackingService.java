package com.st3.uber.service;

import com.st3.uber.domain.*;
import com.st3.uber.dto.ride.*;
import com.st3.uber.dto.route.RouteInfo;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class RideTrackingService {

    private final RideRepository rideRepository;
    private final RideInviteRepository rideInviteRepository;
    private final InconsistencyReportRepository inconsistencyReportRepository;
    private final RouteCalculationService routeCalculationService;

    public RideTrackingService(
            RideRepository rideRepository,
            RideInviteRepository rideInviteRepository,
            InconsistencyReportRepository inconsistencyReportRepository,
            RouteCalculationService routeCalculationService
    ) {
        this.rideRepository = rideRepository;
        this.rideInviteRepository = rideInviteRepository;
        this.inconsistencyReportRepository = inconsistencyReportRepository;
        this.routeCalculationService = routeCalculationService;
    }

    /**
     * Get current active ride for a passenger
     */
    public Optional<RideTrackingResponse> getCurrentRideForPassenger(Passenger passenger) {
        List<Ride> activeRides = rideRepository.findByPassengersContainingAndStatusIn(
                passenger,
                List.of(RideStatus.PENDING, RideStatus.ACCEPTED, RideStatus.IN_PROGRESS)
        );

        if (activeRides.isEmpty()) {
            return Optional.empty();
        }

        // Priority order: IN_PROGRESS > ACCEPTED > PENDING
        Ride ride = activeRides.stream()
                .sorted((r1, r2) -> {
                    int priority1 = getStatusPriority(r1.getStatus());
                    int priority2 = getStatusPriority(r2.getStatus());

                    if (priority1 != priority2) {
                        return Integer.compare(priority1, priority2);
                    }

                    // If same status, pick most recent
                    return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                })
                .findFirst()
                .orElse(activeRides.get(0));

        return Optional.of(buildTrackingResponse(ride));
    }

    /**
     * Get priority order for ride status (lower = higher priority)
     */
    private int getStatusPriority(RideStatus status) {
        return switch (status) {
            case IN_PROGRESS -> 1;  // Highest priority
            case ACCEPTED -> 2;
            case PENDING -> 3;      // Lowest priority
            default -> 99;
        };
    }

    /**
     * Validate tracking token
     */
    public TrackingTokenValidationResponse validateToken(String token) {
        Optional<RideInvite> inviteOpt = rideInviteRepository.findByTrackingToken(token);

        if (inviteOpt.isEmpty()) {
            return new TrackingTokenValidationResponse(false, "Invalid tracking token");
        }

        Ride ride = inviteOpt.get().getRide();
        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            return new TrackingTokenValidationResponse(false, "Ride has ended");
        }

        return new TrackingTokenValidationResponse(true, "Valid token");
    }

    /**
     * Get ride by tracking token (for guests)
     */
    public RideTrackingResponse getRideByToken(String token) {
        RideInvite invite = rideInviteRepository.findByTrackingToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invalid tracking token"
                ));

        Ride ride = invite.getRide();
        return buildTrackingResponse(ride);
    }

    /**
     * Report inconsistency for current ride (authenticated passenger)
     */
    @Transactional
    public ReportInconsistencyResponse reportInconsistencyForCurrentRide(
            Passenger passenger,
            ReportInconsistencyRequest request
    ) {
        List<Ride> activeRides = rideRepository.findByPassengersContainingAndStatusIn(
                passenger,
                List.of(RideStatus.PENDING, RideStatus.ACCEPTED, RideStatus.IN_PROGRESS)
        );

        if (activeRides.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active ride found");
        }

        // Priority order: IN_PROGRESS > ACCEPTED > PENDING
        Ride ride = activeRides.stream()
                .sorted((r1, r2) -> {
                    int priority1 = getStatusPriority(r1.getStatus());
                    int priority2 = getStatusPriority(r2.getStatus());

                    if (priority1 != priority2) {
                        return Integer.compare(priority1, priority2);
                    }

                    return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                })
                .findFirst()
                .orElse(activeRides.get(0));

        return createInconsistencyReport(ride, passenger, request);
    }

    /**
     * Report inconsistency by token (guest user)
     */
    @Transactional
    public ReportInconsistencyResponse reportInconsistencyByToken(
            String token,
            ReportInconsistencyRequest request
    ) {
        RideInvite invite = rideInviteRepository.findByTrackingToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invalid tracking token"
                ));

        Ride ride = invite.getRide();
        Passenger rideCreator = ride.getCreator();

        return createInconsistencyReport(ride, rideCreator, request);
    }

    /**
     * FIXED: Create inconsistency report - allows reporting without driver
     */
    private ReportInconsistencyResponse createInconsistencyReport(
            Ride ride,
            Passenger reportedBy,
            ReportInconsistencyRequest request
    ) {
        InconsistencyReport report = new InconsistencyReport();
        report.setReportedBy(reportedBy);
        report.setRide(ride);

        // Only set driver if one is assigned
        if (ride.getDriver() != null) {
            report.setDriver(ride.getDriver());
        }

        report.setReportText(request.reportText());
        report.setCreatedAt(LocalDateTime.now());

        InconsistencyReport savedReport = inconsistencyReportRepository.save(report);

        return new ReportInconsistencyResponse(
                savedReport.getId(),
                ride.getId(),
                "Inconsistency reported successfully",
                savedReport.getCreatedAt()
        );
    }

    /**
     * FIXED: Build tracking response with dynamic time calculation
     */
    private RideTrackingResponse buildTrackingResponse(Ride ride) {
        Driver driver = ride.getDriver();

        String driverName = driver != null
                ? driver.getName() + " " + driver.getSurname()
                : "Not assigned";

        String driverPhone = driver != null ? driver.getPhoneNumber() : null;
        Location driverLocation = driver != null ? driver.getCurrentLocation() : null;

        Vehicle vehicle = driver != null ? driver.getVehicle() : null;
        String vehicleModel = vehicle != null ? vehicle.getModel() : null;
        String vehicleRegistration = vehicle != null ? vehicle.getRegistrationNumber() : null;

        // FIXED: Recalculate route dynamically based on current driver location
        RouteInfo info = calculateDynamicRoute(ride, driverLocation);

        // FIXED: Calculate remaining time based on current position
        int remainingMinutes = calculateRemainingMinutes(ride, info);

        return new RideTrackingResponse(
                ride.getId(),
                ride.getStatus(),
                ride.getStartLocation(),
                ride.getEndLocation(),
                ride.getRideStops(),
                driverLocation,
                driverName,
                driverPhone,
                ride.getVehicleType(),
                vehicleModel,
                vehicleRegistration,
                info.distanceKm(),
                ride.getEstimatedTimeMinutes(),
                remainingMinutes,
                ride.getStartedAt(),
                ride.getScheduledAt(),
                ride.isStoppedEarly(),
                ride.getCreatedAt()
        );
    }

    /**
     * Calculate route dynamically - if driver location exists and ride is in progress,
     * calculate from driver's current location to destination
     */
    private RouteInfo calculateDynamicRoute(Ride ride, Location driverLocation) {
        // If ride is in progress and driver location is known, calculate from current position
        if (ride.getStatus() == RideStatus.IN_PROGRESS && driverLocation != null) {
            return routeCalculationService.calculateRoute(
                    driverLocation,
                    ride.getEndLocation(),
                    ride.getRideStops()
            );
        }

        // Otherwise calculate full route from start to end
        return routeCalculationService.calculateRoute(
                ride.getStartLocation(),
                ride.getEndLocation(),
                ride.getRideStops()
        );
    }

    /**
     * Calculate remaining minutes based on ride status and current position
     */
    private int calculateRemainingMinutes(Ride ride, RouteInfo routeInfo) {
        if (ride.getStatus() == RideStatus.PENDING || ride.getStatus() == RideStatus.ACCEPTED) {
            return 999999; // Special value for "waiting to start"
        }

        if (ride.getStatus() == RideStatus.IN_PROGRESS) {
            // Use the recalculated duration from current position
            return routeInfo.durationMinutes();
        }

        return 0; // Completed or cancelled
    }
}