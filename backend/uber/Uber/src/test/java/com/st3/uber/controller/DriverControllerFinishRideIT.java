package com.st3.uber.controller;

import com.st3.uber.domain.*;
import com.st3.uber.dto.route.RouteInfo;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.UserRole;
import com.st3.uber.enums.VehicleType;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.PassengerRepository;
import com.st3.uber.repository.RideRepository;
import com.st3.uber.repository.UserRepository;
import com.st3.uber.service.RouteCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
  private UserRepository userRepository;

  @MockitoBean
  private RouteCalculationService routeCalculationService;

  private Passenger testPassenger;
  private Driver testDriver;
  private Ride currentRide;
  private Admin testAdmin;

  @BeforeEach
  void setUp() {
    rideRepository.deleteAll();
    driverRepository.deleteAll();
    passengerRepository.deleteAll();

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
    testDriver.setAvailable(false);
    testDriver.setFree(false);
    testDriver.setVehicle(vehicle);

    Location driverLocation = new Location();
    driverLocation.setLat(45.2671);
    driverLocation.setLng(19.8335);
    driverLocation.setAddress("Bulevar oslobođenja");
    testDriver.setCurrentLocation(driverLocation);

    testDriver = driverRepository.save(testDriver);

    Location start = new Location();
    start.setLat(20.2671);
    start.setLng(19.8335);
    start.setAddress("Bulevar oslobođenja 1");

    Location end = new Location();
    end.setLat(45.2702);
    end.setLng(19.8401);
    end.setAddress("Trg slobode");

    currentRide = new Ride();
    currentRide.setStatus(RideStatus.IN_PROGRESS);
    currentRide.setVehicleType(VehicleType.STANDARD);
    currentRide.setStartLocation(start);
    currentRide.setEndLocation(end);
    currentRide.setCreator(testPassenger);

    currentRide.setPassengers(new ArrayList<>(List.of(testPassenger)));
    currentRide.setInvites(new ArrayList<>());
    currentRide.setRideStops(new ArrayList<>());
    currentRide.setActualRideStops(new ArrayList<>());

    currentRide.setEstimatedTimeMinutes(12);
    currentRide.setDistance(3.5);
    currentRide.setBasePrice(100.0);
    currentRide.setCalculatedPrice(520.0);

    currentRide.setDriver(testDriver);
    currentRide = rideRepository.saveAndFlush(currentRide);

    testDriver.setCurrentRide(currentRide);
    testDriver = driverRepository.saveAndFlush(testDriver);

    when(routeCalculationService.calculateRoute(any(), any(), anyList())).thenReturn(new RouteInfo(3.5, 12));

    testAdmin = new Admin();

    testAdmin.setEmail("admin@gmail.com");
    testAdmin.setPassword("pass123");
    testAdmin.setName("Marko");
    testAdmin.setSurname("Markovic");
    testAdmin.setPhoneNumber("123456");
    testAdmin.setAddress("Test address");
    testAdmin.setRole(UserRole.ADMIN);
    testAdmin.setVerified(true);
    testAdmin.setBlocked(false);

    userRepository.save(testAdmin);
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor driverJwt(Long driverId) {
    return jwt()
        .jwt(j -> j.claim("uid", driverId))
        .authorities(new SimpleGrantedAuthority("ROLE_DRIVER"));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor passengerJwt(Long passengerId) {
    return jwt()
        .jwt(j -> j.claim("uid", passengerId))
        .authorities(new SimpleGrantedAuthority("ROLE_PASSENGER"));
  }
  private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt(Long adminId) {
    return jwt()
        .jwt(j -> j.claim("uid", adminId))
        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  static Stream<Arguments> validRideStatuses() {
    return Stream.of(
        Arguments.of(RideStatus.FINISHED_EARLY, new Location(35, 27.5, "Unknown Location")),
        Arguments.of(RideStatus.COMPLETED, new Location(45.2702, 19.8401, "Trg slobode"))
    );
  }

  private String finishRideRequestBody(double lat, double lng, String address) {
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

  @ParameterizedTest
  @MethodSource("validRideStatuses")
  void finish_ride_happy_path_test(RideStatus status, Location actualEndLocation) throws Exception {

    mockMvc.perform(
            post("/api/drivers/rides/{rideId}/finish", currentRide.getId())
                .with(driverJwt(testDriver.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(finishRideRequestBody(
                    actualEndLocation.getLat(),
                    actualEndLocation.getLng(),
                    actualEndLocation.getAddress()
                )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(status.name()))
        .andExpect(jsonPath("$.finishedAt").exists())
        .andExpect(jsonPath("$.finalPrice").exists());

    Ride finishedRide = rideRepository.findById(currentRide.getId()).orElseThrow();
    assertThat(finishedRide.getStatus()).isEqualTo(status);
    assertThat(finishedRide.getActualEndLocation().getLat()).isEqualTo(actualEndLocation.getLat());
    assertThat(finishedRide.getActualEndLocation().getLng()).isEqualTo(actualEndLocation.getLng());
    assertThat(finishedRide.getActualEndLocation().getAddress()).isNotBlank();
  }

  @Test
  void finish_ride_without_token() throws Exception {
    mockMvc.perform(
            post("/api/drivers/rides/{rideId}/finish", currentRide.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(finishRideRequestBody(45.2702, 19.8401, "Trg slobode")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void finish_ride_as_passenger() throws Exception {
    mockMvc.perform(
            post("/api/drivers/rides/{rideId}/finish", currentRide.getId())
                .with(passengerJwt(testPassenger.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(finishRideRequestBody(45.2702, 19.8401, "Trg slobode")))
        .andExpect(status().isForbidden());
  }

  @Test
  void finish_ride_as_admin() throws Exception {
    mockMvc.perform(
            post("/api/drivers/rides/{rideId}/finish", currentRide.getId())
                .with(passengerJwt(testAdmin.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(finishRideRequestBody(45.2702, 19.8401, "Trg slobode")))
        .andExpect(status().isForbidden());
  }

  @Test
  void finish_ride_when_driver_has_no_current_ride() throws Exception {
    testDriver.setCurrentRide(null);
    testDriver.setFree(true);
    driverRepository.save(testDriver);

    mockMvc.perform(
            post("/api/drivers/rides/{rideId}/finish", currentRide.getId())
                .with(adminJwt(testDriver.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(finishRideRequestBody(45.2702, 19.8401, "Trg slobode")))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @MethodSource("invalidRideStatuses")
  void finish_ride_when_ride_not_in_progress(RideStatus status) throws Exception {
    currentRide.setStatus(status);
    rideRepository.save(currentRide);

    mockMvc.perform(
            post("/api/drivers/rides/{rideId}/finish", currentRide.getId())
                .with(driverJwt(testDriver.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(finishRideRequestBody(45.2702, 19.8401, "Trg slobode")))
        .andExpect(status().isBadRequest());
  }

  private static Stream<RideStatus> invalidRideStatuses() {
    return Stream.of(
        RideStatus.ACCEPTED,
        RideStatus.REJECTED,
        RideStatus.CANCELLED_BY_DRIVER,
        RideStatus.CANCELLED_BY_PASSENGER,
        RideStatus.FINISHED_EARLY,
        RideStatus.PENDING,
        RideStatus.COMPLETED
    );
  }
}
