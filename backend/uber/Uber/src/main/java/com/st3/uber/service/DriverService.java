package com.st3.uber.service;

import com.st3.uber.domain.*;
import com.st3.uber.dto.route.RouteInfo;
import com.st3.uber.dto.user.driver.DriverCancelRideRequest;
import com.st3.uber.dto.user.driver.DriverStatusResponse;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.st3.uber.util.GetAddressFromLatLng.addressFromLatLng;
import static org.springframework.http.HttpStatus.*;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final NotificationService notificationService;
    private final RideRepository rideRepository;
    private final RouteCalculationService routeCalculationService;

    private final Map<Long, LocalDateTime> activeSessionStart =
            new ConcurrentHashMap<>();

    private final MailService mailService;

    public DriverService(
        DriverRepository driverRepository,
        NotificationService notificationService,
        RideRepository rideRepository,
        RouteCalculationService routeCalculationService, MailService mailService) {
        this.driverRepository = driverRepository;
        this.notificationService = notificationService;
        this.rideRepository = rideRepository;
        this.routeCalculationService = routeCalculationService;
        this.mailService = mailService;
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

        LocalDateTime start = activeSessionStart.remove(driverId);
        if (start != null) {
            long minutes = Duration.between(start, LocalDateTime.now()).toMinutes();
            driver.setWorkingMinutesPerDay(driver.getWorkingMinutesPerDay() + (int) minutes);
        }

        driver.setActive(false);
        driver.setAvailable(false);
        driver.setFree(false);
        driverRepository.save(driver);
    }

    @Transactional
    public DriverStatusResponse changeActiveStatus(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        boolean inRide = driver.getCurrentRide() != null;

        boolean hasNextRides = !rideRepository
            .findByDriverAndStatusIn(driver, List.of(RideStatus.PENDING, RideStatus.ACCEPTED))
            .isEmpty();

        if (inRide || hasNextRides) {

            if (!driver.isActivityRequest()) {
                driver.setActivityRequest(true);
                driverRepository.save(driver);

                notificationService.createNotification(
                    driverId,
                    inRide
                        ? "Your status change request will be processed after completing the current ride."
                        : "Your status change request will be processed after all scheduled rides are completed.",
                    NotificationType.PROFILE_CHANGE,
                    inRide ? driver.getCurrentRide().getId() : null
                );
            }

            return new DriverStatusResponse(driver.isActive(), true, inRide);
        }

        boolean newActive = !driver.isActive();


        if (newActive) {
            activeSessionStart.put(driverId, LocalDateTime.now());
        } else {
            LocalDateTime start = activeSessionStart.remove(driverId);
            if (start != null) {
                long minutes = Duration.between(start, LocalDateTime.now()).toMinutes();
                int updated = driver.getWorkingMinutesPerDay() + (int) minutes;
                driver.setWorkingMinutesPerDay(Math.min(updated, 480));
            }
        }
        driver.setActive(newActive);
        driver.setAvailable(newActive);
        driver.setFree(newActive);
        driver.setActivityRequest(false);
        driverRepository.save(driver);

        notificationService.createNotification(
            driverId,
            newActive
                ? "You are now active and available for rides."
                : "You are now inactive and will not receive ride requests.",
            NotificationType.PROFILE_CHANGE,
            null
        );

        return new DriverStatusResponse(newActive, false, false);
    }

    @Transactional
    public void changeActiveStatusAfterRequest(Long driverId){
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        if (!driver.isActivityRequest()) {
            return;
        }

        boolean inRide = driver.getCurrentRide() != null;

        boolean hasNextRides = !rideRepository
            .findByDriverAndStatusIn(driver, List.of(RideStatus.PENDING, RideStatus.ACCEPTED))
            .isEmpty();

        if (inRide || hasNextRides) {
            return;
        }

        boolean newActive = !driver.isActive();
        driver.setActive(newActive);
        driver.setAvailable(newActive);
        driver.setFree(newActive);
        driver.setActivityRequest(false);

        driverRepository.save(driver);

        String statusMessage = newActive
                ? "Your status change request has been processed. You are now active and available for rides."
                : "Your status change request has been processed. You are now inactive and will not receive ride requests.";

        notificationService.createNotification(
                driverId,
                statusMessage,
                NotificationType.PROFILE_CHANGE,
                null
        );
     }

    public List<ActiveVehicleResponse> getActiveDriverLocations() {
        List<Driver> activeDrivers = driverRepository.findActiveDrivers();

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

    @Transactional
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
    public Ride finishRide(Long driverId, Location actualEndLocation) {

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        Ride currentRide = driver.getCurrentRide();
        if (currentRide == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Driver has no active ride");
        }

        if (currentRide.getStatus() != RideStatus.IN_PROGRESS) {
            throw new ResponseStatusException(BAD_REQUEST, "Can only finish rides that are in progress");
        }

        String address = addressFromLatLng(actualEndLocation.getLat(), actualEndLocation.getLng());
        actualEndLocation.setAddress(address);

        currentRide.setActualEndLocation(actualEndLocation);

        if (currentRide.getActualRideStops().isEmpty() && !currentRide.getRideStops().isEmpty()) {
            currentRide.getActualRideStops().addAll(currentRide.getRideStops());
        }

        boolean earlyFinish = finishedEarly(currentRide, actualEndLocation);

        if(!earlyFinish)
            currentRide.setStatus(RideStatus.COMPLETED);

        currentRide.setFinishedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.saveAndFlush(currentRide);

        driver.setCurrentRide(null);
        driver.setFree(true);

        List<Ride> nextRides = rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED)
                .stream()
                .filter(r -> r.getDriver() != null && r.getDriver().getId().equals(driverId))
                .sorted(Comparator.comparing(Ride::getScheduledAt))
                .toList();

        sendNotifications(savedRide, currentRide, driver, nextRides);

        driverRepository.save(driver);

        return currentRide;
    }

    private void sendNotifications(Ride savedRide, Ride currentRide, Driver driver, List<Ride> nextRides){
        List<String> status = List.of("is completed", "Completed", "complete");
        if (currentRide.getStatus() == RideStatus.FINISHED_EARLY) {
            status = List.of("finished early", "Finished Early", "finish early");
        }

        for (Passenger passenger : savedRide.getPassengers()) {
            notificationService.createNotification(
                passenger.getId(),
                String.format("Your ride " + status.get(0) + "! Total cost: %.2f RSD. Please rate your experience.",
                    savedRide.getCalculatedPrice()),
                NotificationType.FINISHED_RIDE,
                savedRide.getId()
            );
        }

        for (Passenger passenger : currentRide.getPassengers()) {
            try {
                String subject = "Ride " + status.get(1);
                String body = String.format("""
                Dear %s,
                
                Your ride has been completed.
                
                Ride Details:
                - From: %s
                - To: %s
                - Distance: %.2f km
                - Duration: %d minutes
                - Total Cost: %.2f RSD
                
                Thank you for riding with us!
                
                Best regards,
                Uber Team
                """,
                    passenger.getName(),
                    currentRide.getStartLocation().getAddress(),
                    currentRide.getActualEndLocation() != null ? currentRide.getActualEndLocation().getAddress() : currentRide.getEndLocation().getAddress(),
                    currentRide.getDistance(),
                    currentRide.getEstimatedTimeMinutes(),
                    currentRide.getCalculatedPrice()
                );
                mailService.sendText(passenger.getEmail(), subject, body);
            } catch (Exception e) {
                System.err.println("Failed to send completion email to passenger: " + e.getMessage());
            }
        }

        for (RideInvite invite : currentRide.getInvites()) {
            try {
                String subject = "Ride " + status.get(1) + " - Tracking Update";
                String body = String.format("""
                Hello,
                
                The ride you were tracking has been completed.
                
                Ride Details:
                - From: %s
                - To: %s
                - Distance: %.2f km
                
                Thank you for your interest!
                
                Best regards,
                Uber Team
                """,
                    currentRide.getStartLocation().getAddress(),
                    currentRide.getActualEndLocation() != null ? currentRide.getActualEndLocation().getAddress() : currentRide.getEndLocation().getAddress(),
                    currentRide.getDistance()
                );
                mailService.sendText(invite.getEmail(), subject, body);
            } catch (Exception e) {
                System.err.println("Failed to send completion email to invite: " + e.getMessage());
            }
        }

        if (!nextRides.isEmpty()) {
            Ride nextRide = nextRides.get(0);

            driver.setCurrentRide(nextRide);
            driver.setAvailable(false);

            notificationService.createNotification(
                driver.getId(),
                "Ride " + status.get(1) + "! Your next scheduled ride is now active.",
                NotificationType.RIDE_REMINDER,
                nextRide.getId()
            );
        }
        else {
            driver.setAvailable(true);

            notificationService.createNotification(
                driver.getId(),
                "Ride " + status.get(1) + "! You are now available for new rides.",
                NotificationType.PROFILE_CHANGE,
                null
            );
        }
    }

    private boolean finishedEarly(Ride ride, Location currentLocation){
        if(!isWithinRadius(currentLocation, ride.getEndLocation())){
            ride.setStatus(RideStatus.FINISHED_EARLY);
            RouteInfo routeInfo = routeCalculationService.calculateRoute(
                ride.getStartLocation(),
                currentLocation,
                ride.getActualRideStops()
            );
            ride.setDistance(routeInfo.distanceKm());
            double price = ride.getBasePrice() + ride.getDistance() * 120;
            ride.setCalculatedPrice(price);
            return true;
        }
        return false;
    }

    private boolean isWithinRadius(Location loc1, Location loc2) {
        double distance = DistanceCalculator.distanceKm(
                loc1.getLat(),
                loc1.getLng(),
                loc2.getLat(),
                loc2.getLng()
        );
        return distance <= 0.3;
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
//        driver.setFree(false);

        driverRepository.save(driver);

        return startedRide;
    }

    @Transactional
    public void cancelRideByDriver(Long rideId, Long driverId, DriverCancelRideRequest reason){
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driverId))
            return;

        ride.setStatus(RideStatus.CANCELLED_BY_DRIVER);
        ride.setCancelledAt(LocalDateTime.now());
        ride.setCancelledBy(CancelledBy.DRIVER);
        ride.setTerminationReason(reason.getCancellationReason());

        driver.setActive(true);
        driver.setAvailable(true);
        driver.setFree(true);
        driver.setCurrentRide(null);

        driverRepository.save(driver);
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

//        ride.setStatus(RideStatus.PANIC);
        ride.setPanic(true);
        rideRepository.save(ride);
    }

    public void startDriverSession(Long driverId){
        activeSessionStart.put(driverId, LocalDateTime.now());
    }

    public int getLiveWorkingMinutes(Driver driver) {

        int base = driver.getWorkingMinutesPerDay();

        LocalDateTime start = activeSessionStart.get(driver.getId());

        if (start == null) {
            return base;
        }

        long extra = Duration.between(start, LocalDateTime.now()).toMinutes();

        int total = base + (int) extra;

        return Math.min(total, 480);
    }

}