package com.st3.uber.s2.repository;

import com.st3.uber.domain.*;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.UserRole;
import com.st3.uber.enums.VehicleType;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.PassengerRepository;
import com.st3.uber.repository.RideRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-layer tests for RideRepository — Functionality 2.7 (Finish Ride).
 *
 * Only tests custom-defined methods (not plain inherited JPA ones).
 * Tested methods:
 *   - findByIdWithLock                        (pessimistic write-lock SELECT)
 *   - findByStatusAndScheduledAtIsNotNull      (upcoming scheduled rides lookup)
 *   - findByDriverAndStatusIn                  (driver's rides filtered by status)
 *   - findByDriver                             (all rides for a given driver)
 */
@DataJpaTest
@ActiveProfiles("test")
class RideRepositoryTest {

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private DriverRepository driverRepository;


    private Passenger buildAndSavePassenger() {
        Passenger p = new Passenger();
        p.setEmail("passenger@test.com");
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

    private Driver buildAndSaveDriver(String email, String plate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setModel("Toyota Corolla");
        vehicle.setType(VehicleType.STANDARD);
        vehicle.setRegistrationNumber(plate);
        vehicle.setSeatingCapacity(4);
        vehicle.setBabyTransport(false);
        vehicle.setPetTransport(false);

        Driver d = new Driver();
        d.setEmail(email);
        d.setPassword("pass123");
        d.setName("Petar");
        d.setSurname("Petrovic");
        d.setPhoneNumber("654321");
        d.setAddress("Driver address");
        d.setRole(UserRole.DRIVER);
        d.setVerified(true);
        d.setBlocked(false);
        d.setActive(true);
        d.setAvailable(true);
        d.setFree(true);
        d.setVehicle(vehicle);
        return driverRepository.save(d);
    }

    private Ride buildAndSaveRide(Passenger creator, Driver driver,
                                  RideStatus status, LocalDateTime scheduledAt) {
        Ride r = new Ride();
        r.setCreator(creator);
        r.setStatus(status);
        r.getPassengers().add(creator);
        r.setStartLocation(new Location(45.0, 19.0, "Start"));
        r.setEndLocation(new Location(45.1, 19.1, "End"));
        r.setCreatedAt(LocalDateTime.now());
        r.setEstimatedTimeMinutes(15);
        r.setVehicleType(VehicleType.STANDARD);
        r.setDistance(5.0);
        r.setBasePrice(100.0);
        r.setCalculatedPrice(700.0);
        r.setBabyTransport(false);
        r.setPetTransport(false);
        r.setDriver(driver);
        r.setScheduledAt(scheduledAt);
        return rideRepository.save(r);
    }


    @Test
    void findByIdWithLock_shouldReturnRide_whenExists() {
        // A ride saved to the DB must be retrievable via the lock query
        Passenger passenger = buildAndSavePassenger();
        Ride saved = buildAndSaveRide(passenger, null, RideStatus.IN_PROGRESS, null);

        Optional<Ride> found = rideRepository.findByIdWithLock(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
    }

    @Test
    void findByIdWithLock_shouldReturnEmpty_whenRideDoesNotExist() {
        // A non-existent ID must yield an empty Optional, not throw
        Optional<Ride> found = rideRepository.findByIdWithLock(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void findByIdWithLock_shouldReturnCorrectRide_whenMultipleRidesExist() {
        // The lock query must not accidentally return the wrong ride
        Passenger passenger = buildAndSavePassenger();
        Ride rideA = buildAndSaveRide(passenger, null, RideStatus.IN_PROGRESS, null);
        Ride rideB = buildAndSaveRide(passenger, null, RideStatus.COMPLETED,   null);

        Optional<Ride> found = rideRepository.findByIdWithLock(rideA.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(rideA.getId());
        assertThat(found.get().getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
    }


    @Test
    void findByStatusAndScheduledAtIsNotNull_shouldReturnRide_whenScheduledAndStatusMatches() {
        // An ACCEPTED ride with a non-null scheduledAt must appear in results
        Passenger passenger = buildAndSavePassenger();
        Driver driver = buildAndSaveDriver("driver@test.com", "NS-001-AA");
        LocalDateTime future = LocalDateTime.now().plusHours(1);

        buildAndSaveRide(passenger, driver, RideStatus.ACCEPTED, future);

        List<Ride> result = rideRepository
                .findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScheduledAt()).isNotNull();
    }

    @Test
    void findByStatusAndScheduledAtIsNotNull_shouldReturnEmpty_whenScheduledAtIsNull() {
        // A ride with ACCEPTED status but no scheduledAt must NOT be included
        Passenger passenger = buildAndSavePassenger();
        buildAndSaveRide(passenger, null, RideStatus.ACCEPTED, null); // scheduledAt = null

        List<Ride> result = rideRepository
                .findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED);

        assertThat(result).isEmpty();
    }

    @Test
    void findByStatusAndScheduledAtIsNotNull_shouldReturnEmpty_whenStatusDoesNotMatch() {
        // A scheduled ride with PENDING status must not appear when querying ACCEPTED
        Passenger passenger = buildAndSavePassenger();
        LocalDateTime future = LocalDateTime.now().plusHours(1);
        buildAndSaveRide(passenger, null, RideStatus.PENDING, future);

        List<Ride> result = rideRepository
                .findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED);

        assertThat(result).isEmpty();
    }

    @Test
    void findByStatusAndScheduledAtIsNotNull_shouldReturnAll_whenMultipleMatch() {
        // All matching scheduled rides must be returned, not just the first one
        Passenger passenger = buildAndSavePassenger();
        Driver driver = buildAndSaveDriver("driver@test.com", "NS-001-AA");

        buildAndSaveRide(passenger, driver, RideStatus.ACCEPTED, LocalDateTime.now().plusHours(1));
        buildAndSaveRide(passenger, driver, RideStatus.ACCEPTED, LocalDateTime.now().plusHours(2));

        List<Ride> result = rideRepository
                .findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED);

        assertThat(result).hasSize(2);
    }


    @Test
    void findByDriverAndStatusIn_shouldReturnMatchingRide_whenStatusInList() {
        // An ACCEPTED ride assigned to the driver must be found
        Passenger passenger = buildAndSavePassenger();
        Driver driver = buildAndSaveDriver("driver@test.com", "NS-001-AA");
        buildAndSaveRide(passenger, driver, RideStatus.ACCEPTED, null);

        List<Ride> result = rideRepository.findByDriverAndStatusIn(
                driver, List.of(RideStatus.PENDING, RideStatus.ACCEPTED));

        assertThat(result).hasSize(1);
    }

    @Test
    void findByDriverAndStatusIn_shouldReturnEmpty_whenStatusNotInList() {
        // A COMPLETED ride must not appear when filtering for PENDING / ACCEPTED
        Passenger passenger = buildAndSavePassenger();
        Driver driver = buildAndSaveDriver("driver@test.com", "NS-001-AA");
        buildAndSaveRide(passenger, driver, RideStatus.COMPLETED, null);

        List<Ride> result = rideRepository.findByDriverAndStatusIn(
                driver, List.of(RideStatus.PENDING, RideStatus.ACCEPTED));

        assertThat(result).isEmpty();
    }

    @Test
    void findByDriverAndStatusIn_shouldReturnEmpty_whenRideBelongsToDifferentDriver() {
        // Rides assigned to another driver must not pollute the result
        Passenger passenger = buildAndSavePassenger();
        Driver driver1 = buildAndSaveDriver("driver1@test.com", "NS-001-AA");
        Driver driver2 = buildAndSaveDriver("driver2@test.com", "NS-002-BB");

        buildAndSaveRide(passenger, driver2, RideStatus.ACCEPTED, null);

        List<Ride> result = rideRepository.findByDriverAndStatusIn(
                driver1, List.of(RideStatus.ACCEPTED));

        assertThat(result).isEmpty();
    }

    @Test
    void findByDriverAndStatusIn_shouldReturnRidesForBothStatusValues() {
        // Both a PENDING and an ACCEPTED ride for the same driver must be returned
        Passenger passenger = buildAndSavePassenger();
        Driver driver = buildAndSaveDriver("driver@test.com", "NS-001-AA");

        buildAndSaveRide(passenger, driver, RideStatus.PENDING,  null);
        buildAndSaveRide(passenger, driver, RideStatus.ACCEPTED, null);

        List<Ride> result = rideRepository.findByDriverAndStatusIn(
                driver, List.of(RideStatus.PENDING, RideStatus.ACCEPTED));

        assertThat(result).hasSize(2);
    }

    @Test
    void findByDriver_shouldReturnAllRides_forGivenDriver() {
        // Both rides assigned to the driver must be found regardless of status
        Passenger passenger = buildAndSavePassenger();
        Driver driver = buildAndSaveDriver("driver@test.com", "NS-001-AA");

        buildAndSaveRide(passenger, driver, RideStatus.IN_PROGRESS, null);
        buildAndSaveRide(passenger, driver, RideStatus.COMPLETED,   null);

        List<Ride> result = rideRepository.findByDriver(driver);

        assertThat(result).hasSize(2);
    }

    @Test
    void findByDriver_shouldReturnEmpty_whenDriverHasNoRides() {
        // A driver with no rides at all must get an empty list, not an exception
        Driver driver = buildAndSaveDriver("driver@test.com", "NS-001-AA");

        List<Ride> result = rideRepository.findByDriver(driver);

        assertThat(result).isEmpty();
    }

    @Test
    void findByDriver_shouldNotIncludeRidesFromOtherDrivers() {
        // Querying driver1 must not return rides that belong to driver2
        Passenger passenger = buildAndSavePassenger();
        Driver driver1 = buildAndSaveDriver("driver1@test.com", "NS-001-AA");
        Driver driver2 = buildAndSaveDriver("driver2@test.com", "NS-002-BB");

        buildAndSaveRide(passenger, driver2, RideStatus.COMPLETED, null);

        List<Ride> result = rideRepository.findByDriver(driver1);

        assertThat(result).isEmpty();
    }
}