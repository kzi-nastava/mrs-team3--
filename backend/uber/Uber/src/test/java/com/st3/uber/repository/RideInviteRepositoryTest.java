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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RideInviteRepositoryTest {

    @Autowired
    private RideInviteRepository rideInviteRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private RideRepository rideRepository;



    private Passenger buildPassenger() {
        Passenger p = new Passenger();

        p.setEmail("test@gmail.com");
        p.setPassword("pass");
        p.setName("Marko");
        p.setSurname("Markovic");
        p.setPhoneNumber("123");
        p.setAddress("Address");
        p.setRole(UserRole.PASSENGER);
        p.setVerified(true);

        return passengerRepository.save(p);
    }

    private Ride buildRide(Passenger creator) {

        Ride r = new Ride();

        r.setCreator(creator);
        r.setStatus(RideStatus.PENDING);

        r.getPassengers().add(creator);

        r.setStartLocation(new Location(45,19,"Start"));
        r.setEndLocation(new Location(46,20,"End"));

        r.setCreatedAt(LocalDateTime.now());
        r.setEstimatedTimeMinutes(10);

        r.setVehicleType(VehicleType.STANDARD);

        r.setDistance(5);
        r.setBasePrice(100);
        r.setCalculatedPrice(150);

        r.setBabyTransport(false);
        r.setPetTransport(false);

        return rideRepository.save(r);
    }


    @Test
    void findByTrackingToken_shouldReturnInvite_whenExists() {

        Passenger p = buildPassenger();
        Ride r = buildRide(p);

        RideInvite invite = new RideInvite();
        invite.setRide(r);
        invite.setEmail("invite@gmail.com");
        invite.setTrackingToken("TOKEN123");
        invite.setCreatedAt(LocalDateTime.now());

        rideInviteRepository.save(invite);

        Optional<RideInvite> found =
                rideInviteRepository.findByTrackingToken("TOKEN123");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("invite@gmail.com");
    }


    @Test
    void findByTrackingToken_shouldReturnEmpty_whenNotExists() {

        Optional<RideInvite> found =
                rideInviteRepository.findByTrackingToken("NOPE");

        assertThat(found).isEmpty();
    }
}
