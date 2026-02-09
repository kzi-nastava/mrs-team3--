package com.st3.uber.service;

import com.st3.uber.domain.*;
import com.st3.uber.dto.user.admin.AdminRideTrackingDto;
import com.st3.uber.dto.route.RouteInfo;
import com.st3.uber.dto.route.StopStatus;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.RideRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class AdminRideService {

    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final RouteCalculationService routeCalculationService;

    public AdminRideService(
            RideRepository rideRepository,
            DriverRepository driverRepository,
            RouteCalculationService routeCalculationService
    ) {
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
        this.routeCalculationService = routeCalculationService;
    }

    public AdminRideTrackingDto getDriverCurrentRide(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Driver not found"
                ));

        Ride currentRide = driver.getCurrentRide();

        if (currentRide == null) {
            return null;
        }

        return mapToAdminRideTrackingDto(currentRide, driver);
    }


    public AdminRideTrackingDto getRideById(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ride not found"
                ));

        return mapToAdminRideTrackingDto(ride, ride.getDriver());
    }


    private AdminRideTrackingDto mapToAdminRideTrackingDto(Ride ride, Driver driver) {
        // Map passengers
        List<AdminRideTrackingDto.PassengerInfo> passengerInfos = ride.getPassengers().stream()
                .map(p -> new AdminRideTrackingDto.PassengerInfo(
                        p.getId(),
                        p.getName(),
                        p.getSurname(),
                        p.getEmail()
                ))
                .collect(Collectors.toList());

        // Map invites
        List<AdminRideTrackingDto.RideInviteInfo> inviteInfos = ride.getInvites().stream()
                .map(i -> new AdminRideTrackingDto.RideInviteInfo(
                        i.getId(),
                        i.getEmail(),
                        i.getTrackingToken(),
                        i.getCreatedAt()
                ))
                .collect(Collectors.toList());

        // Map stop statuses
        List<StopStatus> stopStatuses = IntStream
                .range(0, ride.getRideStops().size())
                .mapToObj(index -> {
                    Location stop = ride.getRideStops().get(index);
                    boolean reached = ride.getActualRideStops().stream()
                            .anyMatch(s -> s.getLat().equals(stop.getLat())
                                    && s.getLng().equals(stop.getLng()));
                    return new StopStatus(index, stop, reached, null);
                })
                .collect(Collectors.toList());

        // Get driver current location
        Location driverCurrentLocation = null;
        if (driver != null && driver.getCurrentLocation() != null) {
            driverCurrentLocation = driver.getCurrentLocation();
        }

        // Recalculate route dynamically based on current driver location
        RouteInfo info = calculateDynamicRoute(ride, driverCurrentLocation);

        // Calculate remaining time based on current position
        int remainingMinutes = calculateRemainingMinutes(ride, info);

        // Get creator info
        String creatorName = ride.getCreator() != null
                ? ride.getCreator().getName() + " " + ride.getCreator().getSurname()
                : "Unknown";

        Long creatorId = ride.getCreator() != null ? ride.getCreator().getId() : null;

        // Driver info
        String driverName = driver != null ? driver.getName() + " " + driver.getSurname() : "Not assigned";
        String driverEmail = driver != null ? driver.getEmail() : null;
        String driverPhone = driver != null ? driver.getPhoneNumber() : null;
        Long driverId = driver != null ? driver.getId() : null;

        // Vehicle info
        String vehicleModel = driver != null && driver.getVehicle() != null
                ? driver.getVehicle().getModel()
                : null;
        String vehicleRegistration = driver != null && driver.getVehicle() != null
                ? driver.getVehicle().getRegistrationNumber()
                : null;

        return new AdminRideTrackingDto(
                ride.getId(),
                ride.getStatus(),
                ride.getStartLocation(),
                ride.getEndLocation(),
                ride.getRideStops(),
                stopStatuses,
                driverCurrentLocation,
                driverId,
                driverName,
                driverPhone,
                driverEmail,
                ride.getVehicleType(),
                vehicleModel,
                vehicleRegistration,
                info.distanceKm(),
                ride.getEstimatedTimeMinutes(),
                remainingMinutes,
                ride.getCalculatedPrice(),
                ride.getCreatedAt(),
                ride.getScheduledAt(),
                ride.getStartedAt(),
                ride.getFinishedAt(),
                ride.isBabyTransport(),
                ride.isPetTransport(),
                passengerInfos,
                creatorId,
                creatorName,
                inviteInfos,
                ride.isPanic()
        );
    }


    private RouteInfo calculateDynamicRoute(Ride ride, Location driverLocation) {
        // If ride is in progress and driver location is known, calculate remaining route
        if (ride.getStatus() == RideStatus.IN_PROGRESS && driverLocation != null) {
            // Get only the stops that haven't been reached yet
            List<Location> unreachedStops = ride.getRideStops().stream()
                    .filter(plannedStop -> {
                        // Check if this stop has been reached (exists in actualRideStops)
                        return ride.getActualRideStops().stream()
                                .noneMatch(actualStop ->
                                        actualStop.getLat().equals(plannedStop.getLat()) &&
                                                actualStop.getLng().equals(plannedStop.getLng())
                                );
                    })
                    .toList();

            // Calculate route from current position through unreached stops to destination
            return routeCalculationService.calculateRoute(
                    driverLocation,
                    ride.getEndLocation(),
                    unreachedStops
            );
        }

        // Otherwise calculate full route from start to end with all planned stops
        return routeCalculationService.calculateRoute(
                ride.getStartLocation(),
                ride.getEndLocation(),
                ride.getRideStops()
        );
    }


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