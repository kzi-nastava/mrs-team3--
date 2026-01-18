package com.st3.uber.service;

import com.st3.uber.domain.Location;
import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.domain.RideInvite;
import com.st3.uber.dto.ride.CreateRideRequest;
import com.st3.uber.dto.route.RouteInfo;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.VehicleType;
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
import java.util.UUID;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final PassengerRepository passengerRepository;
    private final RideInviteRepository rideInviteRepository;
    private final RouteCalculationService routeCalculationService;
    private final PriceCalculationService priceCalculationService;


    public RideService(
            RideRepository rideRepository,
            PassengerRepository passengerRepository,
            RideInviteRepository rideInviteRepository,
            RouteCalculationService routeCalculationService,
            PriceCalculationService priceCalculationService
    ) {
        this.rideRepository = rideRepository;
        this.passengerRepository = passengerRepository;
        this.rideInviteRepository = rideInviteRepository;
        this.routeCalculationService = routeCalculationService;
        this.priceCalculationService = priceCalculationService;
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

        return rideRepository.save(ride);
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

        return rideRepository.save(ride);
    }


    public RouteEstimateResponse estimateRoute(RouteEstimateRequest request) {

        Location start = new Location(
                request.getStartLocation().latitude(),
                request.getStartLocation().longitude(),
                request.getStartLocation().address()
        );

        // END
        Location end = new Location(
                request.getEndLocation().latitude(),
                request.getEndLocation().longitude(),
                request.getEndLocation().address()
        );

        // STOPS
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

}
