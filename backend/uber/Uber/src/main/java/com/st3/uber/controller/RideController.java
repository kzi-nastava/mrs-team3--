package com.st3.uber.controller;

import com.st3.uber.dto.ride.CreateRideFromFavoriteRequest;
import com.st3.uber.dto.ride.CreateRideRequest;
import com.st3.uber.dto.ride.RideResponse;
import com.st3.uber.dto.ride.StartRideResponse;
import com.st3.uber.enums.RideStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/rides")
public class RideController {


  @PostMapping(
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )

  public ResponseEntity<RideResponse> createRide(@RequestBody CreateRideRequest request) {

    RideResponse response = new RideResponse(
      100L,
      RideStatus.PENDING,
      8,
      650.0,
      request.vehicleType()
    );

    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }


  @PostMapping(
    value = "/favorites/{favoriteRouteId}",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )

  public ResponseEntity<RideResponse> createRideFromFavorite(@PathVariable Long favoriteRouteId, @RequestBody CreateRideFromFavoriteRequest request) {

    RideResponse response = new RideResponse(
      200L,
      RideStatus.PENDING,
      6,
      550.0,
      request.vehicleType()
    );

    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }


  @PostMapping(
    value = "/{rideId}/start",
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<StartRideResponse> startRide(@PathVariable Long rideId) {

    StartRideResponse response = new StartRideResponse(
      rideId,
      RideStatus.IN_PROGRESS,
      "2025-01-18T14:30:00"
    );

    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
