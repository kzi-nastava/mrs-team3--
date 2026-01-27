package com.st3.uber.service;

import com.st3.uber.domain.*;
import com.st3.uber.dto.vehicle.ActiveVehicleResponse;
import com.st3.uber.enums.NotificationType;
import com.st3.uber.enums.RideRejectReason;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.exception.RideRejectedException;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.RideRepository;
import com.st3.uber.util.DistanceCalculator;
import org.springframework.stereotype.Service;
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
            // NOTIFICATION: Notify all passengers that no drivers are available
            notifyPassengersRideDeclined(ride, "No active drivers are currently available. Please try again later.");
            throw new RideRejectedException(RideRejectReason.NO_ACTIVE_DRIVERS);
        }

        List<Driver> eligibleDrivers = drivers.stream()
                .filter(d -> d.getVehicle().getType() == ride.getVehicleType())
                .filter(d -> !ride.isBabyTransport() || d.getVehicle().isBabyTransport())
                .filter(d -> !ride.isPetTransport() || d.getVehicle().isPetTransport())
                .filter(d -> d.getWorkingMinutesPerDay() < 480)
                .toList();

        if (eligibleDrivers.isEmpty()) {
            // NOTIFICATION: Notify all passengers that no matching drivers found
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
            // NOTIFICATION: Notify all passengers that all drivers are busy
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

    /**
     * Helper method to notify all passengers in a ride that it was declined
     */
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

        if(!driver.getNextRides().isEmpty()){
            throw new ResponseStatusException(FORBIDDEN, "Driver cannot change status because of next rides");
        }

        if(driver.getCurrentRide() != null){
            driver.setActivityRequest(true);
            driverRepository.save(driver);

            // NOTIFICATION: Notify driver that status change is pending
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

        // NOTIFICATION: Notify driver about status change
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
                driver.isAvailable(), // true = green (available), false = red (unavailable)
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

    /**
     * Get pending rides that this driver can accept
     * Already filtered by vehicle capabilities in the query
     */
    public List<Ride> getPendingRidesForDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        if (!driver.isActive() || !driver.isFree()) {
            return List.of(); // Driver not available, return empty list
        }

        Vehicle vehicle = driver.getVehicle();

        return rideRepository.findPendingRidesForDriver(
                vehicle.getType(),
                vehicle.isBabyTransport(),
                vehicle.isPetTransport(),
                true, // requiresBabyTransport flag
                true  // requiresPetTransport flag
        );
    }

    /**
     * Accept a pending ride
     * Updates driver status and calls RideService to accept the ride
     */
    public Ride acceptRide(Long driverId, Long rideId, RideService rideService) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        if (!driver.isActive() || !driver.isFree()) {
            throw new ResponseStatusException(FORBIDDEN, "Driver is not available to accept rides");
        }

        if (driver.getCurrentRide() != null) {
            throw new ResponseStatusException(FORBIDDEN, "Driver already has an active ride");
        }

        // Accept the ride (handled in RideService)
        Ride ride = rideService.acceptRide(rideId, driver);

        // Update driver status
        driver.setCurrentRide(ride);
        driver.setFree(false);
        driver.setAvailable(false);

        driverRepository.save(driver);

        return ride;
    }

    /**
     * Finish current ride
     * Updates driver status and calls RideService to finish the ride
     */
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

        // Finish the ride (handled in RideService)
        Ride finishedRide = rideService.finishRideWithDetails(currentRide.getId(), actualEndLocation);

        // Update driver status
        driver.setCurrentRide(null);

        // Check for next scheduled ride
        List<Ride> nextRides = driver.getNextRides();
        boolean hasNextRide = !nextRides.isEmpty();

        if (hasNextRide) {
            Ride nextRide = nextRides.get(0);
            driver.setCurrentRide(nextRide);
            nextRides.remove(0);
            driver.setFree(false);
            driver.setAvailable(false);

            notificationService.createNotification(
                    driver.getId(),
                    "Ride completed! Your next scheduled ride is now active.",
                    NotificationType.RIDE_REMINDER,
                    nextRide.getId()
            );
        } else {
            driver.setFree(true);
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

    /**
     * Update driver's current location
     * Simple location update during ride
     */
    public void updateDriverLocation(Long driverId, double latitude, double longitude) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        Location location = new Location(latitude, longitude, null);
        driver.setCurrentLocation(location);
        driver.setLocationUpdatedAt(LocalDateTime.now());

        driverRepository.save(driver);
    }

    /**
     * Teleport driver to start position (for testing without GPS)
     */
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

        // Teleport to start
        driver.setCurrentLocation(currentRide.getStartLocation());
        driver.setLocationUpdatedAt(LocalDateTime.now());

        driverRepository.save(driver);
    }

    /**
     * Mark a stop as reached
     * Delegates to RideService for ride logic
     */
    public Ride reachStop(Long driverId, int stopIndex, RideService rideService) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        Ride currentRide = driver.getCurrentRide();
        if (currentRide == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Driver has no active ride");
        }

        return rideService.reachStop(currentRide.getId(), stopIndex);
    }
}