package com.st3.uber.s2.repository;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Location;
import com.st3.uber.domain.Vehicle;
import com.st3.uber.enums.UserRole;
import com.st3.uber.enums.VehicleType;
import com.st3.uber.repository.DriverRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-layer tests for DriverRepository — Functionality 2.7 (Finish Ride).
 *
 * Tested custom methods (not plain inherited JPA):
 *   - findAvailableDrivers   (@Query: active=true, available=true, free=true, no currentRide)
 *   - findActiveDrivers      (@Query: active=true)
 */
@DataJpaTest
@ActiveProfiles("test")
class DriverRepositoryTest {

    @Autowired
    private DriverRepository driverRepository;

    private Driver buildAndSaveDriver(String email, String plate,
                                      boolean active, boolean available, boolean free) {
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
        d.setActive(active);
        d.setAvailable(available);
        d.setFree(free);
        d.setVehicle(vehicle);
        return driverRepository.save(d);
    }


    @Test
    void findAvailableDrivers_shouldReturnDriver_whenAllFlagsTrue() {
        // A fully available driver (active, available,free no ride) must be found
        buildAndSaveDriver("driver@test.com", "NS-001-AA", true, true, true);

        List<Driver> result = driverRepository.findAvailableDrivers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActive()).isTrue();
        assertThat(result.get(0).isAvailable()).isTrue();
        assertThat(result.get(0).isFree()).isTrue();
    }

    @Test
    void findAvailableDrivers_shouldReturnEmpty_whenDriverIsInactive() {
        // active=false means the driver is logged out — must not appear
        buildAndSaveDriver("driver@test.com", "NS-001-AA", false, true, true);

        List<Driver> result = driverRepository.findAvailableDrivers();

        assertThat(result).isEmpty();
    }

    @Test
    void findAvailableDrivers_shouldReturnEmpty_whenDriverIsNotFree() {
        // free=false means they are currently handling a ride — must not appear
        buildAndSaveDriver("driver@test.com", "NS-001-AA", true, true, false);

        List<Driver> result = driverRepository.findAvailableDrivers();

        assertThat(result).isEmpty();
    }

    @Test
    void findAvailableDrivers_shouldReturnEmpty_whenDriverIsUnavailable() {
        // available=false is set while the driver is en route to a pickup
        buildAndSaveDriver("driver@test.com", "NS-001-AA", true, false, true);

        List<Driver> result = driverRepository.findAvailableDrivers();

        assertThat(result).isEmpty();
    }

    @Test
    void findAvailableDrivers_shouldReturnOnlyFullyAvailableDrivers_whenMixed() {
        // Only the driver with all flags true and no current ride must be returned
        buildAndSaveDriver("available@test.com",   "NS-001-AA", true,  true,  true);
        buildAndSaveDriver("busy@test.com",         "NS-002-BB", true,  false, false);
        buildAndSaveDriver("inactive@test.com",     "NS-003-CC", false, true,  true);

        List<Driver> result = driverRepository.findAvailableDrivers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("available@test.com");
    }

    @Test
    void findAvailableDrivers_shouldReturnMultiple_whenSeveralAreAvailable() {
        // All fully-available drivers must be returned, not just the first one
        buildAndSaveDriver("driver1@test.com", "NS-001-AA", true, true, true);
        buildAndSaveDriver("driver2@test.com", "NS-002-BB", true, true, true);

        List<Driver> result = driverRepository.findAvailableDrivers();

        assertThat(result).hasSize(2);
    }


    @Test
    void findActiveDrivers_shouldReturnDriver_whenActive() {
        buildAndSaveDriver("driver@test.com", "NS-001-AA", true, false, false);

        List<Driver> result = driverRepository.findActiveDrivers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActive()).isTrue();
    }

    @Test
    void findActiveDrivers_shouldReturnEmpty_whenAllInactive() {
        buildAndSaveDriver("driver@test.com", "NS-001-AA", false, false, false);

        List<Driver> result = driverRepository.findActiveDrivers();

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveDrivers_shouldIncludeDriversRegardlessOfFreeOrAvailable() {
        // An active driver who is busy (free=false, available=false) still appears
        buildAndSaveDriver("busy@test.com",  "NS-001-AA", true, false, false);
        buildAndSaveDriver("free@test.com",  "NS-002-BB", true, true,  true);

        List<Driver> result = driverRepository.findActiveDrivers();

        assertThat(result).hasSize(2);
    }

    @Test
    void findActiveDrivers_shouldNotIncludeInactiveDrivers() {
        // Only active drivers must appear even when inactive ones also exist
        buildAndSaveDriver("active@test.com",   "NS-001-AA", true,  true, true);
        buildAndSaveDriver("inactive@test.com", "NS-002-BB", false, true, true);

        List<Driver> result = driverRepository.findActiveDrivers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("active@test.com");
    }

}