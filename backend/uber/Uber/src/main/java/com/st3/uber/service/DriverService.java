package com.st3.uber.service;

import com.st3.uber.domain.*;
import com.st3.uber.dto.user.driver.DriverCancelRideRequest;
import com.st3.uber.dto.vehicle.ActiveVehicleResponse;
import com.st3.uber.enums.CancelledBy;
import com.st3.uber.enums.NotificationType;
import com.st3.uber.enums.RideRejectReason;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.exception.RideRejectedException;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.RideRepository;
import com.st3.uber.util.DistanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final NotificationService notificationService;
    private final RideRepository rideRepository;

    public DriverService(
            DriverRepository driverRepository,
            NotificationService notificationService,
            RideRepository rideRepository
    ) {
        this.driverRepository = driverRepository;
        this.notificationService = notificationService;
        this.rideRepository = rideRepository;
    }

    public List<Driver> findAvailableDrivers() {
        return driverRepository.findAvailableDrivers();
    }

    public Driver findDriverForRide(Ride ride) {
        List<Driver> drivers = driverRepository.findAvailableDrivers();

        if (drivers.isEmpty()) {
            notifyPassengersRideDeclined(ride, "No active drivers are currently available. Please try again later.");
            throw new RideRejectedException(RideRejectReason.NO_ACTIVE_DRIVERS);
        }

        List<Driver> eligibleDrivers = drivers.stream()
                .filter(d -> !d.isBlocked())
                .filter(d -> d.getVehicle().getType() == ride.getVehicleType())
                .filter(d -> !ride.isBabyTransport() || d.getVehicle().isBabyTransport())
                .filter(d -> !ride.isPetTransport() || d.getVehicle().isPetTransport())
                .filter(d -> d.getWorkingMinutesPerDay() < 480)
                .toList();

        if (eligibleDrivers.isEmpty()) {
            notifyPassengersRideDeclined(ride, "No drivers match your ride requirements (vehicle type, baby/pet transport). Please adjust your request.");
            throw new RideRejectedException(RideRejectReason.NO_MATCHING_DRIVERS);
        }

        List<Driver> freeDrivers = eligibleDrivers.stream()
                .filter(Driver::isFree)
                .toList();

        List<Driver> almostFreeDrivers = eligibleDrivers.stream()
                .filter(d -> !d.isFree())
                .filter(d -> d.getCurrentRide() != null)
                .filter(d -> d.getCurrentRide().getRemainingMinutes() <= 10)
                .toList();

        List<Driver> candidates;

        if (!freeDrivers.isEmpty()) {
            candidates = freeDrivers;
        } else if (!almostFreeDrivers.isEmpty()) {
            candidates = almostFreeDrivers;
        } else {
            notifyPassengersRideDeclined(ride, "All drivers are currently busy. Please try again in a few minutes.");
            throw new RideRejectedException(RideRejectReason.NO_ACTIVE_DRIVERS);
        }

        Location pickup = ride.getStartLocation();

        Driver selectedDriver = candidates.stream()
                .filter(d ->
                        d.getCurrentLocation() != null &&
                                d.getCurrentLocation().getLat() != null &&
                                d.getCurrentLocation().getLng() != null
                )
                .min(Comparator.comparingDouble(d ->
                        DistanceCalculator.distanceKm(
                                pickup.getLat(),
                                pickup.getLng(),
                                d.getCurrentLocation().getLat(),
                                d.getCurrentLocation().getLng()
                        )
                ))
                .orElse(null);

        if (selectedDriver == null) {
            // NOTIFICATION: Notify all passengers that no driver has location
            notifyPassengersRideDeclined(ride, "Unable to find drivers with active location tracking. Please try again.");
            throw new RideRejectedException(RideRejectReason.NO_DRIVER_WITH_LOCATION);
        }

        return selectedDriver;
    }


    private void notifyPassengersRideDeclined(Ride ride, String reason) {
        for (Passenger passenger : ride.getPassengers()) {
            notificationService.createNotification(
                    passenger.getId(),
                    reason,
                    NotificationType.DECLINED_RIDE,
                    null
            );
        }
    }

    public void logoutDriver(Long driverId){
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        if (driver.getCurrentRide() != null) {
            throw new ResponseStatusException(FORBIDDEN, "Driver cannot logout while in a ride");
        }

        driver.setActive(false);
        driver.setAvailable(false);
        driver.setFree(false);
        driverRepository.save(driver);
    }

    public void changeActiveStatus(Long driverId){
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        List<Ride> scheduledRides = rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.PENDING)
                .stream()
                .filter(r -> r.getDriver() != null && r.getDriver().getId().equals(driverId))
                .toList();

        if(!scheduledRides.isEmpty()){
            throw new ResponseStatusException(FORBIDDEN, "Driver cannot change status because of next rides");
        }

        if(driver.getCurrentRide() != null){
            driver.setActivityRequest(true);
            driverRepository.save(driver);

            notificationService.createNotification(
                    driverId,
                    "Your status change request will be processed after completing the current ride.",
                    NotificationType.PROFILE_CHANGE,
                    driver.getCurrentRide().getId()
            );
            return;
        }

        boolean newActive = !driver.isActive();
        driver.setActive(newActive);
        driver.setAvailable(newActive);
        driver.setFree(newActive);

        driverRepository.save(driver);

        String statusMessage = newActive
                ? "You are now active and available for rides."
                : "You are now inactive and will not receive ride requests.";

        notificationService.createNotification(
                driverId,
                statusMessage,
                NotificationType.PROFILE_CHANGE,
                null
        );
    }


    public List<ActiveVehicleResponse> getActiveDriverLocations() {
        List<Driver> activeDrivers = driverRepository.findActiveAndFreeDrivers();

        return activeDrivers.stream()
                .filter(driver -> driver.getCurrentLocation() != null)
                .filter(driver -> driver.getVehicle() != null)
                .map(this::mapToActiveVehicleResponse)
                .collect(Collectors.toList());
    }

    private ActiveVehicleResponse mapToActiveVehicleResponse(Driver driver) {
        return new ActiveVehicleResponse(
                driver.getCurrentLocation().getLat(),
                driver.getCurrentLocation().getLng(),
                driver.isAvailable(),
                driver.getVehicle().getRegistrationNumber()
        );
    }




    public List<Ride> getDriverRides(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        List<Ride> rides = rideRepository.findByDriver(driver);

        return rides.stream()
                .filter(r -> r.getStatus() == RideStatus.PENDING
                        || r.getStatus() == RideStatus.ACCEPTED
                        || r.getStatus() == RideStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(r -> {
                    if (r.getStatus() == RideStatus.IN_PROGRESS) return 0;
                    if (r.getStatus() == RideStatus.ACCEPTED) return 1;
                    return 2;
                }))
                .collect(Collectors.toList());
    }

    public List<Ride> getPendingRidesForDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        if (!driver.isActive() || !driver.isFree()) {
            return List.of();
        }

        Vehicle vehicle = driver.getVehicle();

        return rideRepository.findPendingRidesForDriver(
                vehicle.getType(),
                vehicle.isBabyTransport(),
                vehicle.isPetTransport(),
                true,
                true
        );
    }


    public Ride acceptRide(Long driverId, Long rideId, RideService rideService) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        if (!driver.isActive() || !driver.isFree()) {
            throw new ResponseStatusException(FORBIDDEN, "Driver is not available to accept rides");
        }

        if (driver.getCurrentRide() != null) {
            throw new ResponseStatusException(FORBIDDEN, "Driver already has an active ride");
        }

        Ride ride = rideService.acceptRide(rideId, driver);

        driver.setCurrentRide(ride);
        driver.setFree(true);
        driver.setAvailable(false);

        driverRepository.save(driver);

        return ride;
    }

    @Transactional
    public Ride finishRide(Long driverId, Location actualEndLocation, RideService rideService) {

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        Ride currentRide = driver.getCurrentRide();
        if (currentRide == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Driver has no active ride");
        }

        if (currentRide.getStatus() != RideStatus.IN_PROGRESS) {
            throw new ResponseStatusException(BAD_REQUEST, "Can only finish rides that are in progress");
        }

        Ride finishedRide = rideService.finishRideWithDetails(
                currentRide.getId(),
                actualEndLocation
        );

        driver.setCurrentRide(null);
        driver.setFree(true);

        List<Ride> nextRides = rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.PENDING)
                .stream()
                .filter(r -> r.getDriver() != null && r.getDriver().getId().equals(driverId))
                .sorted(Comparator.comparing(Ride::getScheduledAt))
                .toList();

        if (!nextRides.isEmpty()) {
            Ride nextRide = nextRides.get(0);

            driver.setCurrentRide(nextRide);
            driver.setAvailable(false);

            notificationService.createNotification(
                    driver.getId(),
                    "Ride completed! Your next scheduled ride is now active.",
                    NotificationType.RIDE_REMINDER,
                    nextRide.getId()
            );
        } else {
            driver.setAvailable(true);

            notificationService.createNotification(
                    driver.getId(),
                    "Ride completed! You are now available for new rides.",
                    NotificationType.PROFILE_CHANGE,
                    null
            );
        }

        driverRepository.save(driver);

        return finishedRide;
    }

    public void updateDriverLocation(Long driverId, double latitude, double longitude) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        Location location = new Location(latitude, longitude, null);
        driver.setCurrentLocation(location);
        driver.setLocationUpdatedAt(LocalDateTime.now());

        driverRepository.save(driver);
    }

    public void moveToStartPosition(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        Ride currentRide = driver.getCurrentRide();
        if (currentRide == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Driver has no active ride");
        }

        if (currentRide.getStatus() != RideStatus.ACCEPTED) {
            throw new ResponseStatusException(BAD_REQUEST, "Can only move to start for accepted rides");
        }

        driver.setCurrentLocation(currentRide.getStartLocation());
        driver.setLocationUpdatedAt(LocalDateTime.now());

        driverRepository.save(driver);
    }

    public Ride reachStop(Long driverId, int stopIndex, RideService rideService) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        Ride currentRide = driver.getCurrentRide();
        if (currentRide == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Driver has no active ride");
        }

        return rideService.reachStop(currentRide.getId(), stopIndex);
    }

    @Transactional
    public Ride startRide(Long driverId, Long rideId, RideService rideService) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        Ride currentRide = driver.getCurrentRide();

        if (currentRide == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Driver has no active ride");
        }

        if (!currentRide.getId().equals(rideId)) {
            throw new ResponseStatusException(FORBIDDEN, "This ride does not belong to the driver");
        }

        if (currentRide.getStatus() != RideStatus.ACCEPTED) {
            throw new ResponseStatusException(BAD_REQUEST, "Ride must be ACCEPTED to start");
        }

        Ride startedRide = rideService.startRide(rideId);

        driver.setAvailable(false);
        driver.setFree(false);

        driverRepository.save(driver);

        return startedRide;
    }

    @Transactional
    public void cancelRideByDriver(Long rideId, Long driverId, DriverCancelRideRequest reason){
        driverRepository.findById(driverId)
            .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driverId))
            return;

        ride.setStatus(RideStatus.CANCELLED_BY_DRIVER);
        ride.setCancelledAt(LocalDateTime.now());
        ride.setCancelledBy(CancelledBy.DRIVER);
        ride.setTerminationReason(reason.getCancellationReason());
        rideRepository.save(ride);
    }

    @Transactional
    public void panicRideByDriver(Long rideId, Long driverId){
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (ride.getDriver() != driver || !ride.getDriver().getId().equals(driverId))
            return;

        ride.setStatus(RideStatus.PANIC);
        ride.setPanic(true);
        rideRepository.save(ride);
    }
}