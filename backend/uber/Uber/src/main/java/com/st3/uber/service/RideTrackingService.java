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
import java.util.List;
import java.util.Optional;

@Service
public class RideTrackingService {

    private final RideRepository rideRepository;
    private final RideInviteRepository rideInviteRepository;
    private final InconsistencyReportRepository inconsistencyReportRepository;
    private final RouteCalculationService  routeCalculationService;

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

        Ride ride = activeRides.get(0);
        return Optional.of(buildTrackingResponse(ride));
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

        Ride ride = activeRides.get(0);
        return createInconsistencyReport(ride, passenger, request);
    }

    /**
     * Report inconsistency by token (guest user)
     * Guest reports are saved with the ride CREATOR as reportedBy
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

        // Guest reports use the ride creator as reportedBy
        Passenger rideCreator = ride.getCreator();

        return createInconsistencyReport(ride, rideCreator, request);
    }

    /**
     * Helper method to create inconsistency report
     */
    private ReportInconsistencyResponse createInconsistencyReport(
            Ride ride,
            Passenger reportedBy,
            ReportInconsistencyRequest request
    ) {
        if (ride.getDriver() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ride has no assigned driver"
            );
        }

        InconsistencyReport report = new InconsistencyReport();
        report.setReportedBy(reportedBy);
        report.setRide(ride);
        report.setDriver(ride.getDriver());
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
     * Helper method to build tracking response
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


        RouteInfo info = routeCalculationService.calculateRoute(ride.getStartLocation(),ride.getEndLocation(),ride.getRideStops());

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
                info.durationMinutes(),
                ride.getStartedAt(),
                ride.getScheduledAt(),
                ride.isStoppedEarly(),
                ride.getCreatedAt()
        );
    }
}