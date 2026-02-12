package com.st3.uber.controller;

import com.st3.uber.domain.*;
import com.st3.uber.dto.location.LocationRequest;
import com.st3.uber.dto.ride.CreateRideRequest;
import com.st3.uber.enums.UserRole;
import com.st3.uber.enums.VehicleType;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.PassengerRepository;
import com.st3.uber.repository.RidePricingRepository;
import com.st3.uber.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RideControllerIT {

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

    @Autowired
    private RidePricingRepository ridePricingRepository;

    private Passenger testPassenger;
    private Driver testDriver;

    @BeforeEach
    void setUp() {
        ridePricingRepository.deleteAll();
        rideRepository.deleteAll();
        driverRepository.deleteAll();
        passengerRepository.deleteAll();

        RidePricing pricing = new RidePricing();
        pricing.setPricePerKm(120.0);

        Map<VehicleType, Double> basePrices = new HashMap<>();
        basePrices.put(VehicleType.STANDARD, 100.0);
        pricing.setBasePrices(basePrices);

        ridePricingRepository.save(pricing);

        testPassenger = new Passenger();
        testPassenger.setEmail("test@gmail.com");
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
        testDriver.setEmail("driver@gmail.com");
        testDriver.setPassword("pass123");
        testDriver.setName("Petar");
        testDriver.setSurname("Petrovic");
        testDriver.setPhoneNumber("654321");
        testDriver.setAddress("Driver address");
        testDriver.setRole(UserRole.DRIVER);
        testDriver.setVerified(true);
        testDriver.setBlocked(false);
        testDriver.setActive(true);
        testDriver.setAvailable(true);
        testDriver.setFree(true);
        testDriver.setVehicle(vehicle);

        Location driverLocation = new Location();
        driverLocation.setLat(45.2671);
        driverLocation.setLng(19.8335);
        driverLocation.setAddress("Bulevar oslobođenja");
        testDriver.setCurrentLocation(driverLocation);

        testDriver = driverRepository.save(testDriver);
    }

    @Test
    void createRide_shouldReturnCreatedRide_whenValidRequest() throws Exception {
        CreateRideRequest request = getCreateRideRequest();

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/rides")
                        .with(jwt()
                                .jwt(jwt -> jwt.claim("uid", testPassenger.getId()))
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rideId").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.vehicleType").value("STANDARD"));

        assertThat(rideRepository.count()).isEqualTo(1);
    }

    private static CreateRideRequest getCreateRideRequest() {
        LocationRequest startLocation = new LocationRequest(
                45.2671,
                19.8335,
                "Bulevar oslobođenja 1"
        );

        LocationRequest endLocation = new LocationRequest(
                45.2702,
                19.8401,
                "Trg slobode"
        );

        return new CreateRideRequest(
                startLocation,
                endLocation,
                List.of(),
                List.of(),
                VehicleType.STANDARD,
                false,
                false,
                null
        );
    }

    @Test
    void createRide_shouldReturn409_whenPassengerBlocked() throws Exception {
        testPassenger.setBlocked(true);
        passengerRepository.save(testPassenger);

        CreateRideRequest request = getCreateRideRequest();

        mockMvc.perform(post("/api/rides")
                        .with(jwt().jwt(jwt -> jwt.claim("uid", testPassenger.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

}