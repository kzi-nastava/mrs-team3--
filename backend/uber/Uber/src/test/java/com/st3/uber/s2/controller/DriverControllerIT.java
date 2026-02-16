package com.st3.uber.s2.controller;

import com.st3.uber.domain.*;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.UserRole;
import com.st3.uber.enums.VehicleType;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.PassengerRepository;
import com.st3.uber.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for POST /api/drivers/rides/{rideId}/finish
 * — Functionality 2.7: Završetak vožnje (Finish Ride).
 *
 *
 * IMPORTANT — JWT roles:
 * @PreAuthorize("hasRole('DRIVER')")
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DriverControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private RideRepository rideRepository;

    private Passenger testPassenger;
    private Driver    testDriver;
    private Ride      testRide;

    @BeforeEach
    void setUp() {
        rideRepository.deleteAll();
        driverRepository.deleteAll();
        passengerRepository.deleteAll();

        testPassenger = new Passenger();
        testPassenger.setEmail("passenger@test.com");
        testPassenger.setPassword("pass123");
        testPassenger.setName("Marko");
        testPassenger.setSurname("Markovic");
        testPassenger.setPhoneNumber("123456");
        testPassenger.setAddress("Test address");
        testPassenger.setRole(UserRole.PASSENGER);
        testPassenger.setVerified(true);
        testPassenger.setBlocked(false);
        testPassenger = passengerRepository.save(testPassenger);

        Vehicle vehicle = new Vehicle();
        vehicle.setModel("Toyota Corolla");
        vehicle.setType(VehicleType.STANDARD);
        vehicle.setRegistrationNumber("NS-123-AB");
        vehicle.setSeatingCapacity(4);
        vehicle.setBabyTransport(false);
        vehicle.setPetTransport(false);

        testDriver = new Driver();
        testDriver.setEmail("driver@test.com");
        testDriver.setPassword("pass123");
        testDriver.setName("Petar");
        testDriver.setSurname("Petrovic");
        testDriver.setPhoneNumber("654321");
        testDriver.setAddress("Driver address");
        testDriver.setRole(UserRole.DRIVER);
        testDriver.setVerified(true);
        testDriver.setBlocked(false);
        testDriver.setActive(true);
        testDriver.setAvailable(false);   // busy — currently in a ride
        testDriver.setFree(false);
        testDriver.setVehicle(vehicle);

        Location driverLocation = new Location();
        driverLocation.setLat(45.2671);
        driverLocation.setLng(19.8335);
        driverLocation.setAddress("Bulevar oslobodjenja");
        testDriver.setCurrentLocation(driverLocation);
        testDriver = driverRepository.save(testDriver);

        testRide = new Ride();
        testRide.setCreator(testPassenger);
        testRide.getPassengers().add(testPassenger);
        testRide.setDriver(testDriver);
        testRide.setStatus(RideStatus.IN_PROGRESS);
        testRide.setStartLocation(new Location(45.2671, 19.8335, "Bulevar oslobodjenja 1"));
        testRide.setEndLocation(new Location(45.2702, 19.8401, "Trg slobode"));
        testRide.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        testRide.setStartedAt(LocalDateTime.now().minusMinutes(20));
        testRide.setEstimatedTimeMinutes(15);
        testRide.setVehicleType(VehicleType.STANDARD);
        testRide.setDistance(5.0);
        testRide.setBasePrice(100.0);
        testRide.setCalculatedPrice(700.0);
        testRide.setBabyTransport(false);
        testRide.setPetTransport(false);
        testRide = rideRepository.save(testRide);

        testDriver.setCurrentRide(testRide);
        testDriver = driverRepository.save(testDriver);
    }


    /**
     * Builds a JWT post-processor for a driver.
     * Includes ROLE_DRIVER authority so @PreAuthorize("hasRole('DRIVER')") passes.
     */
    private org.springframework.test.web.servlet.request.RequestPostProcessor driverJwt(Long driverId) {
        return jwt()
                .jwt(j -> j.claim("uid", driverId))
                .authorities(new SimpleGrantedAuthority("ROLE_DRIVER"));
    }

    /**
     * Builds a JWT post-processor for a passenger.
     * Only ROLE_PASSENGER — will be rejected by the DRIVER-only endpoint.
     */
    private org.springframework.test.web.servlet.request.RequestPostProcessor passengerJwt(Long passengerId) {
        return jwt()
                .jwt(j -> j.claim("uid", passengerId))
                .authorities(new SimpleGrantedAuthority("ROLE_PASSENGER"));
    }


    /** Serialises a FinishRideRequest body with the given end coordinates. */
    private String finishBody(double lat, double lng, String address) {
        Map<String, Object> loc = new HashMap<>();
        loc.put("lat", lat);
        loc.put("lng", lng);
        loc.put("address", address);

        Map<String, Object> body = new HashMap<>();
        body.put("actualEndLocation", loc);

        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void finishRide_happyPath_shouldReturn200WithRideData() throws Exception {
        // Driver finishes at the planned endpoint → 200 OK with all response fields present.
        mockMvc.perform(
                        post("/api/drivers/rides/{rideId}/finish", testRide.getId())
                                .with(driverJwt(testDriver.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(finishBody(45.2702, 19.8401, "Trg slobode")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.finishedAt").exists())
                .andExpect(jsonPath("$.finalPrice").exists());
    }

    @Test
    void finishRide_happyPath_rideStatusShouldBeCompleted() throws Exception {
        // Finishing at the planned destination must flip the status to COMPLETED
        mockMvc.perform(
                        post("/api/drivers/rides/{rideId}/finish", testRide.getId())
                                .with(driverJwt(testDriver.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(finishBody(45.2702, 19.8401, "Trg slobode")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void finishRide_happyPath_shouldPersistFinishedAtInDatabase() throws Exception {
        // After a successful finish the DB record must have finishedAt set
        mockMvc.perform(
                        post("/api/drivers/rides/{rideId}/finish", testRide.getId())
                                .with(driverJwt(testDriver.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(finishBody(45.2702, 19.8401, "Trg slobode")))
                .andExpect(status().isOk());

        Ride saved = rideRepository.findById(testRide.getId()).orElseThrow();
        assertThat(saved.getFinishedAt()).isNotNull();
    }

    @Test
    void finishRide_happyPath_shouldFreeDriverInDatabase() throws Exception {
        // After finishing the driver must have currentRide=null and free=true
        mockMvc.perform(
                        post("/api/drivers/rides/{rideId}/finish", testRide.getId())
                                .with(driverJwt(testDriver.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(finishBody(45.2702, 19.8401, "Trg slobode")))
                .andExpect(status().isOk());

        Driver updated = driverRepository.findById(testDriver.getId()).orElseThrow();
        assertThat(updated.getCurrentRide()).isNull();
        assertThat(updated.isFree()).isTrue();
    }

    @Test
    void finishRide_happyPath_shouldReturnHasNextRideFalse_whenNoUpcomingRides() throws Exception {
        // With no upcoming scheduled rides the response must say hasNextRide=false
        mockMvc.perform(
                        post("/api/drivers/rides/{rideId}/finish", testRide.getId())
                                .with(driverJwt(testDriver.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(finishBody(45.2702, 19.8401, "Trg slobode")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNextRide").value(false));
    }


    @Test
    void finishRide_exactlyAtPlannedEnd_shouldBeCompleted() throws Exception {
        mockMvc.perform(
                        post("/api/drivers/rides/{rideId}/finish", testRide.getId())
                                .with(driverJwt(testDriver.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(finishBody(
                                        testRide.getEndLocation().getLat(),
                                        testRide.getEndLocation().getLng(),
                                        testRide.getEndLocation().getAddress())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void finishRide_shouldReturnHasNextRideTrue_whenDriverHasScheduledRide() throws Exception {
        // When there is an upcoming ACCEPTED scheduled ride for this driver,
        // hasNextRide must be true and nextRideId must point to that ride
        Ride nextRide = new Ride();
        nextRide.setCreator(testPassenger);
        nextRide.getPassengers().add(testPassenger);
        nextRide.setDriver(testDriver);
        nextRide.setStatus(RideStatus.ACCEPTED);
        nextRide.setScheduledAt(LocalDateTime.now().plusHours(1));
        nextRide.setStartLocation(new Location(45.2671, 19.8335, "Start"));
        nextRide.setEndLocation(new Location(45.2702, 19.8401, "End"));
        nextRide.setCreatedAt(LocalDateTime.now());
        nextRide.setEstimatedTimeMinutes(10);
        nextRide.setVehicleType(VehicleType.STANDARD);
        nextRide.setDistance(3.0);
        nextRide.setBasePrice(100.0);
        nextRide.setCalculatedPrice(460.0);
        nextRide.setBabyTransport(false);
        nextRide.setPetTransport(false);
        rideRepository.save(nextRide);

        mockMvc.perform(
                        post("/api/drivers/rides/{rideId}/finish", testRide.getId())
                                .with(driverJwt(testDriver.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(finishBody(45.2702, 19.8401, "Trg slobode")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNextRide").value(true))
                .andExpect(jsonPath("$.nextRideId").value(nextRide.getId()));
    }


    @Test
    void finishRide_withoutToken_shouldReturn401() throws Exception {
        // No JWT at all → Spring Security rejects the request as unauthenticated
        mockMvc.perform(
                        post("/api/drivers/rides/{rideId}/finish", testRide.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(finishBody(45.2702, 19.8401, "Trg slobode")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void finishRide_withPassengerRole_shouldReturn403() throws Exception {
        // A valid token but with only ROLE_PASSENGER → @PreAuthorize blocks it
        mockMvc.perform(
                        post("/api/drivers/rides/{rideId}/finish", testRide.getId())
                                .with(passengerJwt(testPassenger.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(finishBody(45.2702, 19.8401, "Trg slobode")))
                .andExpect(status().isForbidden());
    }

    @Test
    void finishRide_whenDriverHasNoCurrentRide_shouldReturn400() throws Exception {
        // Detach the driver from the ride so the service throws BAD_REQUEST
        testDriver.setCurrentRide(null);
        testDriver.setFree(true);
        driverRepository.save(testDriver);

        mockMvc.perform(
                        post("/api/drivers/rides/{rideId}/finish", testRide.getId())
                                .with(driverJwt(testDriver.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(finishBody(45.2702, 19.8401, "Trg slobode")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void finishRide_whenRideIsNotInProgress_shouldReturn400() throws Exception {
        // A ride that hasn't been started (ACCEPTED) cannot be finished
        testRide.setStatus(RideStatus.ACCEPTED);
        testRide.setStartedAt(null);
        rideRepository.save(testRide);

        mockMvc.perform(
                        post("/api/drivers/rides/{rideId}/finish", testRide.getId())
                                .with(driverJwt(testDriver.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(finishBody(45.2702, 19.8401, "Trg slobode")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void finishRide_whenRideIsAlreadyCompleted_shouldReturn400() throws Exception {
        // A ride that was already finished cannot be finished a second time
        testRide.setStatus(RideStatus.COMPLETED);
        testRide.setFinishedAt(LocalDateTime.now().minusMinutes(5));
        rideRepository.save(testRide);

        mockMvc.perform(
                        post("/api/drivers/rides/{rideId}/finish", testRide.getId())
                                .with(driverJwt(testDriver.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(finishBody(45.2702, 19.8401, "Trg slobode")))
                .andExpect(status().isBadRequest());
    }


    @Test
    void finishRide_shouldLeaveExactlyOneFinishedRideInDatabase() throws Exception {
        // Only one ride must have finishedAt set after a single finish call
        mockMvc.perform(
                        post("/api/drivers/rides/{rideId}/finish", testRide.getId())
                                .with(driverJwt(testDriver.getId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(finishBody(45.2702, 19.8401, "Trg slobode")))
                .andExpect(status().isOk());

        long finishedCount = rideRepository.findAll().stream()
                .filter(r -> r.getFinishedAt() != null)
                .count();

        assertThat(finishedCount).isEqualTo(1);
    }
}