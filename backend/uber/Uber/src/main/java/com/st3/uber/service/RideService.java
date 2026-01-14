package com.st3.uber.service;

import com.st3.uber.domain.Location;
import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.dto.ride.CreateRideRequest;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.PassengerRepository;
import com.st3.uber.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final PassengerRepository passengerRepository;

    public RideService(
            RideRepository rideRepository,
            PassengerRepository passengerRepository
    ) {
        this.rideRepository = rideRepository;
        this.passengerRepository = passengerRepository;
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

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        Ride ride = new Ride();

        ride.setCreator(passenger);
        ride.getPassengers().add(passenger);

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

        ride.setVehicleType(request.vehicleType());
        ride.setStatus(RideStatus.PENDING);
        ride.setCreatedAt(LocalDateTime.now());

        // minimalno za KT2
        ride.setDistance(0);
        ride.setBasePrice(0);
        ride.setCalculatedPrice(0);

        return rideRepository.save(ride);
    }
}
