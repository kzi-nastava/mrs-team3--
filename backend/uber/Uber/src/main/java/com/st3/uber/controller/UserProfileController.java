package com.st3.uber.controller;

import com.st3.uber.dto.user.UpdateUserProfileRequest;
import com.st3.uber.dto.user.admin.AdminProfileResponse;
import com.st3.uber.dto.user.driver.DriverProfileResponse;
import com.st3.uber.dto.user.passenger.PassengerProfileResponse;
import com.st3.uber.dto.vehicle.VehicleResponse;
import com.st3.uber.enums.VehicleType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/users/profile")
public class UserProfileController {


  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getProfile() {
    String role = "DRIVER"; // Radi testiranja mockup podataka ovde samo treba da se izmeni uloga

    if ("DRIVER".equals(role)) {

      VehicleResponse vehicle = new VehicleResponse(
        10L,
        "Toyota Corolla",
        VehicleType.STANDARD
      );

      DriverProfileResponse response = new DriverProfileResponse(
        1L,
        "driver@test.com",
        "Marko",
        "Markovic",
        "+381641234567",
        "Novi Sad",
        vehicle,
        true
      );

      return new ResponseEntity<>(response, HttpStatus.OK);
    }

    if ("PASSENGER".equals(role)) {

      PassengerProfileResponse response = new PassengerProfileResponse(
        2L,
        "passenger@test.com",
        "Ana",
        "Anić",
        "+381601112223",
        "Beograd"
      );

      return new ResponseEntity<>(response, HttpStatus.OK);
    }


    AdminProfileResponse response = new AdminProfileResponse(
      3L,
      "admin@test.com",
      "Petar",
      "Petrović"
    );

    return new ResponseEntity<>(response, HttpStatus.OK);
  }


  @PutMapping(
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<?> updateProfile(@RequestBody UpdateUserProfileRequest req) {

    VehicleResponse vehicle = new VehicleResponse(
      10L,
      "Toyota Corolla",
      VehicleType.STANDARD
    );

    DriverProfileResponse response = new DriverProfileResponse(
      1L,
      "driver@test.com",
      req.firstName(),
      req.lastName(),
      req.phoneNumber(),
      req.address(),
      vehicle,
      true
    );

    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
