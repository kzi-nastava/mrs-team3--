package com.st3.uber.service;

import com.st3.uber.domain.*;
import com.st3.uber.dto.ride.*;
import com.st3.uber.dto.route.RouteInfo;
import com.st3.uber.enums.CancelledBy;
import com.st3.uber.enums.NotificationType;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.PassengerRepository;
import com.st3.uber.repository.RideInviteRepository;
import com.st3.uber.repository.RideRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.st3.uber.dto.route.RouteEstimateRequest;
import com.st3.uber.dto.route.RouteEstimateResponse;
import com.st3.uber.dto.location.LocationRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final PassengerRepository passengerRepository;
    private final RideInviteRepository rideInviteRepository;
    private final RouteCalculationService routeCalculationService;
    private final PriceCalculationService priceCalculationService;
    private final DriverService driverService;
    private final RideInviteMailService rideInviteMailService;
    private final NotificationService notificationService;

    public RideService(
            RideRepository rideRepository,
            PassengerRepository passengerRepository,
            RideInviteRepository rideInviteRepository,
            RouteCalculationService routeCalculationService,
            PriceCalculationService priceCalculationService,
            DriverService driverService,
            RideInviteMailService rideInviteMailService,
            NotificationService notificationService) {
        this.rideRepository = rideRepository;
        this.passengerRepository = passengerRepository;
        this.rideInviteRepository = rideInviteRepository;
        this.routeCalculationService = routeCalculationService;
        this.priceCalculationService = priceCalculationService;
        this.driverService = driverService;
        this.rideInviteMailService = rideInviteMailService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Ride startRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new IllegalStateException("Ride must be ACCEPTED to start");
        }

        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        for (Passenger passenger : savedRide.getPassengers()) {
            notificationService.createNotification(
                    passenger.getId(),
                    "Your ride is now in progress!",
                    NotificationType.RIDE_REMINDER,
                    savedRide.getId()
            );
        }

        return savedRide;
    }

    public Ride createRide(Long passengerId, CreateRideRequest request) {
        Passenger creator = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        LocalDateTime now = LocalDateTime.now();

        if (request.scheduledAt() != null) {

            if (request.scheduledAt().isBefore(now)) {
                throw new IllegalStateException("Scheduled time cannot be in the past");
            }

            if (request.scheduledAt().isAfter(now.plusHours(5))) {
                throw new IllegalStateException("Ride can be scheduled at most 5 hours ahead");
            }
        }

        if (rideRepository.existsByCreatorAndStatusIn(
                creator,
                List.of(RideStatus.PENDING, RideStatus.IN_PROGRESS, RideStatus.ACCEPTED)
        )) {
            throw new IllegalStateException("You already have an active or pending ride");
        }

        Ride ride = new Ride();
        ride.setCreator(creator);
        ride.getPassengers().add(creator);

        ride.setStartLocation(new Location(
                request.startLocation().latitude(),
                request.startLocation().longitude(),
                request.startLocation().address()
        ));

        ride.setEndLocation(new Location(
                request.endLocation().latitude(),
                request.endLocation().longitude(),
                request.endLocation().address()
        ));

        if (request.stops() != null && !request.stops().isEmpty()) {
            request.stops().forEach(s ->
                    ride.getRideStops().add(
                            new Location(
                                    s.latitude(),
                                    s.longitude(),
                                    s.address()
                            )
                    )
            );
        }

        if (request.passengerEmails() != null && !request.passengerEmails().isEmpty()) {
            for (String email : request.passengerEmails()) {
                if (email.equalsIgnoreCase(creator.getEmail())) {
                    continue;
                }

                passengerRepository.findByEmail(email)
                        .ifPresentOrElse(
                                passenger -> {
                                    if (!ride.getPassengers().contains(passenger)) {
                                        ride.getPassengers().add(passenger);
                                    }
                                },
                                () -> {
                                    RideInvite invite = new RideInvite();
                                    invite.setRide(ride);
                                    invite.setEmail(email);
                                    invite.setTrackingToken(UUID.randomUUID().toString());
                                    invite.setCreatedAt(LocalDateTime.now());
                                    ride.getInvites().add(invite);
                                }
                        );
            }
        }

        ride.setVehicleType(request.vehicleType());
        ride.setBabyTransport(request.babyTransport());
        ride.setPetTransport(request.petTransport());

        Driver driver = null;

        if (request.scheduledAt() == null) {

            // ✅ PRIORITY CHECK ZA SCHEDULED RIDES
            boolean scheduledSoon = rideRepository
                    .existsByStatusAndScheduledAtBetween(
                            RideStatus.PENDING,
                            LocalDateTime.now(),
                            LocalDateTime.now().plusMinutes(30)
                    );

            if (scheduledSoon) {
                throw new IllegalStateException(
                        "Drivers reserved for scheduled rides. Try again shortly."
                );
            }

            // instant ride → assign driver now
            driver = driverService.findDriverForRide(ride);

            ride.setDriver(driver);
            driver.setCurrentRide(ride);
            driver.setFree(false);
            driver.setAvailable(false);

            ride.setStatus(RideStatus.ACCEPTED);
        }
        else {
            // scheduled ride
            ride.setStatus(RideStatus.PENDING);
        }


        RouteInfo routeInfo = routeCalculationService.calculateRoute(
                ride.getStartLocation(),
                ride.getEndLocation(),
                ride.getRideStops()
        );

        ride.setDistance(routeInfo.distanceKm());
        ride.setEstimatedTimeMinutes(routeInfo.durationMinutes());

        double basePrice = priceCalculationService.getBasePrice(ride.getVehicleType());
        double calculatedPrice = priceCalculationService.calculatePrice(
                ride.getVehicleType(),
                routeInfo.distanceKm()
        );

        ride.setBasePrice(basePrice);
        ride.setCalculatedPrice(calculatedPrice);
        ride.setCreatedAt(LocalDateTime.now());
        ride.setScheduledAt(request.scheduledAt());
        Ride savedRide = rideRepository.save(ride);

        for (RideInvite invite : savedRide.getInvites()) {
            rideInviteMailService.sendInvite(invite, creator, savedRide);
        }

        if (driver != null) {
            notificationService.createNotification(
                    creator.getId(),
                    String.format("Your ride request has been created. Driver %s %s will pick you up soon!",
                            driver.getName(), driver.getSurname()),
                    NotificationType.ACCEPTED_RIDE,
                    savedRide.getId()
            );
        } else {
            notificationService.createNotification(
                    creator.getId(),
                    "Your ride has been scheduled successfully.",
                    NotificationType.RIDE_REMINDER,
                    savedRide.getId()
            );
        }


        if(driver != null) {
            notificationService.createNotification(
                    driver.getId(),
                    String.format("New ride assigned! Pickup at %s",
                            savedRide.getStartLocation().getAddress()),
                    NotificationType.RIDE_REMINDER,
                    savedRide.getId()
            );

        }
        return savedRide;
    }



    public RouteEstimateResponse estimateRoute(RouteEstimateRequest request) {
        Location start = new Location(
                request.getStartLocation().latitude(),
                request.getStartLocation().longitude(),
                request.getStartLocation().address()
        );

        Location end = new Location(
                request.getEndLocation().latitude(),
                request.getEndLocation().longitude(),
                request.getEndLocation().address()
        );

        List<Location> stops = new ArrayList<>();
        if (request.getStops() != null) {
            for (LocationRequest s : request.getStops()) {
                stops.add(new Location(
                        s.latitude(),
                        s.longitude(),
                        s.address()
                ));
            }
        }

        RouteInfo routeInfo = routeCalculationService.calculateRoute(
                start, end, stops
        );

        double calculatedPrice = priceCalculationService.calculatePrice(
                request.getVehicleType(),
                routeInfo.distanceKm()
        );

        return new RouteEstimateResponse(
                routeInfo.distanceKm(),
                routeInfo.durationMinutes(),
                calculatedPrice
        );
    }

    public List<PassengerRideSummaryResponse> getPastPassengerRides(Long id) {
    Passenger passenger = passengerRepository.findById(id).orElseThrow(
        () -> new IllegalArgumentException("Passenger not found."));

    List<Ride> rides = rideRepository.findByCreatorAndFinishedAtIsNotNull(passenger);

      List<Ride> favoriteRides = passenger.getFavoriteRides();
      Set<Long> favoriteRideIds = favoriteRides.stream()
          .map(Ride::getId)
          .collect(Collectors.toSet());

      return rides.stream()
          .map(r -> new PassengerRideSummaryResponse(
              r.getId(),
              r.getStatus(),
              r.getStartLocation(),
              r.getActualEndLocation(),
              r.getStartedAt(),
              r.getFinishedAt(),
              favoriteRideIds.contains(r.getId())
          ))
          .toList();
  }

    public PassengerRideSummaryExtendedResponse getPastRideDetails(Long id, Long rideId) {
        Passenger passenger = passengerRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Passenger not found."));
        Ride ride = rideRepository.findById(rideId).orElseThrow(
                () -> new IllegalArgumentException("Ride not found.")
        );

        if (!ride.getCreator().equals(passenger)) {
            throw new IllegalArgumentException("Passenger did not create this ride.");
        }
        boolean favorite = passenger.getFavoriteRides() != null &&
                passenger.getFavoriteRides().stream().anyMatch(r -> r.getId().equals(ride.getId()));

        return detailsResponse(ride, favorite);
    }

    private static PassengerRideSummaryExtendedResponse detailsResponse(Ride ride, boolean favorite) {
        PassengerRideSummaryExtendedResponse res = new PassengerRideSummaryExtendedResponse();

        res.setId(ride.getId());
        res.setStatus(ride.getStatus());
        res.setStartLocation(ride.getStartLocation());
        res.setEndLocation(ride.getActualEndLocation());
        res.setStartTime(ride.getStartedAt());
        res.setEndTime(ride.getFinishedAt());
        res.setFavorite(favorite);

        res.setStops(ride.getRideStops() == null ? List.of() : ride.getRideStops());

        if (ride.getDriver() == null){
            res.setDriverName("-");
        }
        else{
            Driver driver = ride.getDriver();
            String name = driver.getName() == null ? "" : driver.getName();
            String surname = driver.getSurname() == null ? "" : driver.getSurname();
            res.setDriverName((name + surname).isBlank() ? "-" : name + surname);
        }

        List<Review> reviews = ride.getReviews();
        if (reviews == null || reviews.isEmpty()) {
            res.setDriverReview(null);
            res.setRideReview(null);
        } else {
            res.setDriverReview(calculateReview(reviews.stream().map(Review::getDriverRating).toList()));
            res.setRideReview(calculateReview(reviews.stream().map(Review::getVehicleRating).toList()));
        }
        List<InconsistencyReport> inconsistencyReports = ride.getInconsistencyReports();
        res.setInconsistencyReports(
                inconsistencyReports == null ? List.of() : inconsistencyReports.stream().map(r -> {
                    InconsistencyReportItemResponse dto = new InconsistencyReportItemResponse();
                    dto.setId(r.getId());
                    dto.setReportText(r.getReportText());
                    dto.setCreatedAt(r.getCreatedAt());
                    return dto;
                }).toList()
        );

        return res;
    }

    private static Double calculateReview(List<Integer> values){
        if (values == null || values.isEmpty())
            return null;
        long count = values.stream().filter(Objects::nonNull).count();
        if (count == 0)
            return null;
        int sum = values.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        return sum / (double) count;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Ride finishRideWithDetails(Long rideId, Location actualEndLocation) {
        Ride ride = rideRepository.findByIdWithLock(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only in-progress rides can be finished");
        }

        if (actualEndLocation != null) {
            ride.setActualEndLocation(actualEndLocation);
        } else {
            ride.setActualEndLocation(ride.getEndLocation());
        }

        if (ride.getActualRideStops().isEmpty() && !ride.getRideStops().isEmpty()) {
            ride.getActualRideStops().addAll(ride.getRideStops());
        }

        ride.setStatus(RideStatus.COMPLETED);
        ride.setFinishedAt(LocalDateTime.now());

       Ride savedRide = rideRepository.saveAndFlush(ride);


        for (Passenger passenger : savedRide.getPassengers()) {
            notificationService.createNotification(
                    passenger.getId(),
                    String.format("Your ride is complete! Total cost: %.2f RSD. Please rate your experience.",
                            savedRide.getCalculatedPrice()),
                    NotificationType.FINISHED_RIDE,
                    savedRide.getId()
            );
        }

        return savedRide;
    }

    public Ride reachStop(Long rideId, int stopIndex) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only mark stops during active ride");
        }

        if (stopIndex < 0 || stopIndex >= ride.getRideStops().size()) {
            throw new IllegalArgumentException("Invalid stop index");
        }

        Location plannedStop = ride.getRideStops().get(stopIndex);

        boolean alreadyReached = ride.getActualRideStops().stream()
                .anyMatch(s -> s.getLat().equals(plannedStop.getLat())
                        && s.getLng().equals(plannedStop.getLng()));

        if (!alreadyReached) {
            ride.getActualRideStops().add(plannedStop);
        }

        return rideRepository.save(ride);
    }

    public Ride acceptRide(Long rideId, Driver driver) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (ride.getDriver() != null) {
            throw new IllegalStateException("Ride already has a driver assigned");
        }

        if (ride.getStatus() != RideStatus.PENDING) {
            throw new IllegalStateException("Can only accept pending rides");
        }

        ride.setDriver(driver);
        ride.setStatus(RideStatus.ACCEPTED);

        Ride savedRide = rideRepository.save(ride);

        for (Passenger passenger : savedRide.getPassengers()) {
            notificationService.createNotification(
                    passenger.getId(),
                    String.format("Driver %s %s has accepted your ride! They will arrive soon.",
                            driver.getName(), driver.getSurname()),
                    NotificationType.ACCEPTED_RIDE,
                    savedRide.getId()
            );
        }

        notificationService.createNotification(
                driver.getId(),
                String.format("You've accepted a ride. Pickup at %s",
                        savedRide.getStartLocation().getAddress()),
                NotificationType.RIDE_REMINDER,
                savedRide.getId()
        );

        return savedRide;
    }

    public List<IncomingRideResponse> getAllIncomingRidesForPassenger(Long id) {
        Passenger passenger = passengerRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Passenger not found."));

        List<Ride> rides = rideRepository.findByCreatorAndScheduledAtAfterAndStatusIn(
                passenger,
                LocalDateTime.now(),
                List.of(RideStatus.ACCEPTED, RideStatus.PENDING)
        );

        return rides.stream().map(r -> {
            IncomingRideResponse item = new IncomingRideResponse();
            item.setId(r.getId());
            item.setStartLocation(r.getStartLocation());
            item.setEndLocation(r.getEndLocation());
            item.setStartTime(r.getScheduledAt());
            return item;
        }).toList();

    }

    public void cancelRideByPassenger(Long rideId, Long passengerId){
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        if (!ride.getCreator().equals(passenger))
            return;

        ride.setStatus(RideStatus.CANCELLED_BY_PASSENGER);
        ride.setCancelledAt(LocalDateTime.now());
        ride.setCancelledBy(CancelledBy.PASSENGER);
        rideRepository.save(ride);
    }

    @Scheduled(fixedRate = 60000) // сваки минут
    @Transactional
    public void assignScheduledRides() {

        LocalDateTime now = LocalDateTime.now();

        List<Ride> rides = rideRepository
                .findByStatusAndScheduledAtBefore(
                        RideStatus.PENDING,
                        now.plusMinutes(10) // 10 минута пре старта
                );

        for (Ride ride : rides) {
            if (ride.getDriver() != null) continue;

            try {

                Driver driver = driverService.findDriverForRide(ride);

                ride.setDriver(driver);
                driver.setCurrentRide(ride);
                driver.setFree(false);
                driver.setAvailable(false);
                ride.setStatus(RideStatus.ACCEPTED);

                ride.setStatus(RideStatus.ACCEPTED);

                rideRepository.save(ride);

                // notifications
                for (Passenger p : ride.getPassengers()) {
                    notificationService.createNotification(
                            p.getId(),
                            "Driver assigned for your scheduled ride!",
                            NotificationType.ACCEPTED_RIDE,
                            ride.getId()
                    );
                }

            } catch (Exception ignored) {
                // нема возача тренутно — покушаће следећи минут
            }
        }
    }



}