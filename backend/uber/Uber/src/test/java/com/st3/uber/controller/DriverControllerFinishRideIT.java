package com.st3.uber.controller;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Location;
import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.domain.RidePricing;
import com.st3.uber.domain.Vehicle;
import com.st3.uber.dto.location.LocationRequest;
import com.st3.uber.dto.ride.CreateRideRequest;
import com.st3.uber.enums.RideStatus;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
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
class DriverControllerFinishRideIT {

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
    driverLocation.setAddress("Driver start");
    testDriver.setCurrentLocation(driverLocation);

    testDriver = driverRepository.save(testDriver);
  }

  @Test
  void finishRide_shouldReturnCompletedRide_andNoNextRide_whenValid() throws Exception {
    Long rideId = createRideViaApi(null);

    Ride ride = rideRepository.findById(rideId).orElseThrow();
    ride.setDriver(testDriver);
    ride.setStatus(RideStatus.IN_PROGRESS);
    ride = rideRepository.saveAndFlush(ride);

    testDriver.setCurrentRide(ride);
    testDriver.setFree(false);
    testDriver.setAvailable(false);
    driverRepository.saveAndFlush(testDriver);

    String finishBodyJson = """
            {
              "actualEndLocation": { "lat": 45.2702, "lng": 19.8401 }
            }
            """;

    mockMvc.perform(post("/api/drivers/rides/{rideId}/finish", rideId)
            .with(jwt()
                .jwt(j -> j.claim("uid", testDriver.getId()))
                .authorities(new SimpleGrantedAuthority("ROLE_DRIVER"))
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content(finishBodyJson))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rideId").value(rideId))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.finishedAt").exists())
        .andExpect(jsonPath("$.hasNextRide").value(false))
        .andExpect(jsonPath("$.nextRideId").doesNotExist());

    Driver refreshedDriver = driverRepository.findById(testDriver.getId()).orElseThrow();
    assertThat(refreshedDriver.getCurrentRide()).isNull();
  }

  @Test
  void finishRide_shouldReturnHasNextRideTrue_whenDriverHasAcceptedRideQueued() throws Exception {
    Long ride1Id = createRideViaApi(null);

    Ride ride1 = rideRepository.findById(ride1Id).orElseThrow();
    ride1.setDriver(testDriver);
    ride1.setStatus(RideStatus.IN_PROGRESS);
    ride1 = rideRepository.saveAndFlush(ride1);

    testDriver.setCurrentRide(ride1);
    testDriver.setFree(false);
    testDriver.setAvailable(false);
    driverRepository.saveAndFlush(testDriver);

    Long ride2Id = createRideViaApi(LocalDateTime.now().plusHours(1));
    Ride ride2 = rideRepository.findById(ride2Id).orElseThrow();
    ride2.setDriver(testDriver);
    ride2.setStatus(RideStatus.ACCEPTED);
    rideRepository.saveAndFlush(ride2);

    String finishBodyJson = """
            {
              "actualEndLocation": { "lat": 45.2702, "lng": 19.8401 }
            }
            """;

    mockMvc.perform(post("/api/drivers/rides/{rideId}/finish", ride1Id)
            .with(jwt()
                .jwt(j -> j.claim("uid", testDriver.getId()))
                .authorities(new SimpleGrantedAuthority("ROLE_DRIVER"))
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content(finishBodyJson))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rideId").value(ride1Id))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.hasNextRide").value(true))
        .andExpect(jsonPath("$.nextRideId").value(ride2Id));
  }

  @Test
  void finishRide_shouldReturn400_whenDriverHasNoActiveRide() throws Exception {
    // Ensure driver has no current ride
    testDriver.setCurrentRide(null);
    testDriver.setFree(true);
    testDriver.setAvailable(true);
    driverRepository.saveAndFlush(testDriver);

    String finishBodyJson = """
            {
              "actualEndLocation": { "lat": 45.2702, "lng": 19.8401 }
            }
            """;

    mockMvc.perform(post("/api/drivers/rides/{rideId}/finish", 999999L)
            .with(jwt()
                .jwt(j -> j.claim("uid", testDriver.getId()))
                .authorities(new SimpleGrantedAuthority("ROLE_DRIVER"))
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content(finishBodyJson))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  private Long createRideViaApi(LocalDateTime scheduledAt) throws Exception {
    CreateRideRequest request = getCreateRideRequest(scheduledAt);
    String requestJson = objectMapper.writeValueAsString(request);

    String response = mockMvc.perform(post("/api/rides")
            .with(jwt().jwt(j -> j.claim("uid", testPassenger.getId())))
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rideId").exists())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode root = objectMapper.readTree(response);
    Long rideId = root.get("rideId").asLong();

    assertThat(rideRepository.existsById(rideId)).isTrue();
    return rideId;
  }

  private static CreateRideRequest getCreateRideRequest(LocalDateTime scheduledAt) {
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
        scheduledAt
    );
  }
}
