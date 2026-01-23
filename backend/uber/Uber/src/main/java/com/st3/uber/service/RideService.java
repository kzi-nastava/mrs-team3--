package com.st3.uber.service;

import com.st3.uber.domain.*;
import com.st3.uber.dto.ride.CreateRideRequest;
import com.st3.uber.dto.ride.PassengerRideSummaryExtendedResponse;
import com.st3.uber.dto.ride.PassengerRideSummaryResponse;
import com.st3.uber.dto.route.RouteInfo;
import com.st3.uber.enums.NotificationType;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.PassengerRepository;
import com.st3.uber.repository.RideInviteRepository;
import com.st3.uber.repository.RideRepository;
import org.springframework.stereotype.Service;
import com.st3.uber.dto.route.RouteEstimateRequest;
import com.st3.uber.dto.route.RouteEstimateResponse;
import com.st3.uber.dto.location.LocationRequest;
import java.util.ArrayList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

    public Ride startRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (ride.getStatus() != RideStatus.PENDING &&
                ride.getStatus() != RideStatus.ACCEPTED) {
            throw new IllegalStateException("Ride cannot be started in current status");
        }

        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        // Notify all passengers that ride has started
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

        if (rideRepository.existsByCreatorAndStatusIn(
                creator,
                List.of(RideStatus.PENDING, RideStatus.IN_PROGRESS)
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

        Driver driver = driverService.findDriverForRide(ride);

        ride.setDriver(driver);
        driver.setCurrentRide(ride);
        driver.setFree(false);
        driver.setAvailable(false);

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
        ride.setStatus(RideStatus.PENDING);
        ride.setCreatedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        // Send email invites
        for (RideInvite invite : savedRide.getInvites()) {
            rideInviteMailService.sendInvite(invite, creator, savedRide);
        }

        // NOTIFICATION: Notify passenger that ride was created
        notificationService.createNotification(
                creator.getId(),
                String.format("Your ride request has been created. Driver %s %s will pick you up soon!",
                        driver.getName(), driver.getSurname()),
                NotificationType.ACCEPTED_RIDE,
                savedRide.getId()
        );

        // NOTIFICATION: Notify driver about new ride
        notificationService.createNotification(
                driver.getId(),
                String.format("New ride assigned! Pickup at %s",
                        savedRide.getStartLocation().getAddress()),
                NotificationType.RIDE_REMINDER,
                savedRide.getId()
        );

        return savedRide;
    }

    // Method to finish a ride
    public Ride finishRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only in-progress rides can be finished");
        }

        ride.setStatus(RideStatus.COMPLETED);
        ride.setFinishedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);

        // Notify all passengers
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

    // Method to cancel a ride
//    public Ride cancelRide(Long rideId, Rid cancelledBy, String reason) {
//        Ride ride = rideRepository.findById(rideId)
//                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
//
//        ride.setStatus(RideStatus.CANCELLED);
//        ride.setCancelledBy(cancelledBy);
//        ride.setCancelledAt(LocalDateTime.now());
//        ride.setTerminationReason(reason);
//
//        Ride savedRide = rideRepository.save(ride);
//
//        // Notify all passengers
//        for (Passenger passenger : savedRide.getPassengers()) {
//            notificationService.createNotification(
//                    passenger.getId(),
//                    String.format("Your ride has been canceled by %s. Reason: %s",
//                            cancelledBy, reason != null ? reason : "Not specified"),
//                    NotificationType.RIDE_CANCELED,
//                    savedRide.getId()
//            );
//        }
//
//        // Notify driver if exists
//        if (savedRide.getDriver() != null) {
//            notificationService.createNotification(
//                    savedRide.getDriver().getId(),
//                    String.format("Ride canceled by %s. Reason: %s",
//                            cancelledBy, reason != null ? reason : "Not specified"),
//                    NotificationType.RIDE_CANCELED,
//                    savedRide.getId()
//            );
//        }
//
//        return savedRide;
//    }

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

    List<Ride> rides = rideRepository.findPastByCreator(passenger);

      List<Ride> favoriteRides = rideRepository.getFavoriteRidesByCreator(passenger);
      Set<Long> favoriteRideIds = favoriteRides.stream()
          .map(Ride::getId)
          .collect(Collectors.toSet());

      return rides.stream()
          .map(r -> new PassengerRideSummaryResponse(
              r.getId(),
              r.getStatus(),
              r.getStartLocation(),
              r.getEndLocation(),
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

        if (!ride.getPassengers().contains(passenger)) {
            throw new IllegalArgumentException("Passenger did not participate in this ride.");
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
        res.setEndLocation(ride.getEndLocation());
        res.setStartTime(ride.getStartedAt());
        res.setEndTime(ride.getFinishedAt());
        res.setFavorite(favorite);

        res.setStops(ride.getRideStops());
        res.setDriver(ride.getDriver());
        res.setReviews(ride.getReviews());
        res.setInconsistencyReports(ride.getInconsistencyReports());
        return res;
    }
}