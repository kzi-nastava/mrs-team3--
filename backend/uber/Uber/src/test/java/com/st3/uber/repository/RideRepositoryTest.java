package com.st3.uber.repository;

import com.st3.uber.domain.*;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.UserRole;
import com.st3.uber.enums.VehicleType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RideRepositoryTest {

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private PassengerRepository passengerRepository;


    private Passenger buildPassenger() {
        Passenger p = new Passenger();

        p.setEmail("test@gmail.com");
        p.setPassword("pass123");
        p.setName("Marko");
        p.setSurname("Markovic");
        p.setPhoneNumber("123456");
        p.setAddress("Test address");
        p.setRole(UserRole.PASSENGER);
        p.setVerified(true);
        p.setBlocked(false);

        return passengerRepository.save(p);
    }

    private Ride buildRide(Passenger creator) {

        Ride r = new Ride();

        r.setCreator(creator);
        r.setStatus(RideStatus.PENDING);

        r.getPassengers().add(creator);

        r.setStartLocation(new Location(45.0, 19.0, "Start"));
        r.setEndLocation(new Location(45.1, 19.1, "End"));

        r.setCreatedAt(LocalDateTime.now());
        r.setEstimatedTimeMinutes(10);

        r.setVehicleType(VehicleType.STANDARD);

        r.setDistance(5.0);
        r.setBasePrice(100);
        r.setCalculatedPrice(150);

        r.setBabyTransport(false);
        r.setPetTransport(false);

        return rideRepository.save(r);
    }



    @Test
    void existsByCreatorAndStatusIn_shouldReturnTrue() {

        Passenger p = buildPassenger();

        buildRide(p);

        boolean exists =
                rideRepository.existsByCreatorAndStatusIn(
                        p,
                        List.of(RideStatus.PENDING, RideStatus.ACCEPTED)
                );

        assertThat(exists).isTrue();
    }



    @Test
    void existsByCreatorAndStatusIn_shouldReturnFalse() {

        Passenger p = buildPassenger();

        boolean exists =
                rideRepository.existsByCreatorAndStatusIn(
                        p,
                        List.of(RideStatus.PENDING, RideStatus.ACCEPTED)
                );

        assertThat(exists).isFalse();
    }



    @Test
    void existsByStatusAndScheduledAtBetween_shouldReturnTrue() {

        Passenger p = buildPassenger();

        Ride r = buildRide(p);
        r.setScheduledAt(LocalDateTime.now().plusMinutes(15));

        rideRepository.save(r);

        boolean exists =
                rideRepository.existsByStatusAndScheduledAtBetween(
                        RideStatus.PENDING,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusMinutes(30)
                );

        assertThat(exists).isTrue();
    }



    @Test
    void existsByStatusAndScheduledAtBetween_shouldReturnFalse() {

        Passenger p = buildPassenger();

        Ride r = buildRide(p);
        r.setScheduledAt(LocalDateTime.now().plusHours(5));

        rideRepository.save(r);

        boolean exists =
                rideRepository.existsByStatusAndScheduledAtBetween(
                        RideStatus.PENDING,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusMinutes(30)
                );

        assertThat(exists).isFalse();
    }
}
