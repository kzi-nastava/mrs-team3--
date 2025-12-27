package com.st3.uber.controller;

import com.st3.uber.dto.ride.*;
import com.st3.uber.enums.RideStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/rides")
public class RideController {


  @PostMapping(
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.CREATED)
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
  @ResponseStatus(HttpStatus.CREATED)

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

    // GET /api/rides - Get all rides (with optional filters)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllRides(
            @RequestParam(required = false) RideStatus status,
            @RequestParam(required = false) Long passengerId,
            @RequestParam(required = false) Long driverId
    ) {
        return ResponseEntity.ok().build();
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


    // POST /api/rides/{id}/complete - Complete specific ride
    @PostMapping(
            value = "/{id}/complete",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<StartRideResponse> completeRide(@PathVariable Long id) {
        StartRideResponse response = new StartRideResponse(
                id,
                RideStatus.COMPLETED,
                "2025-01-18T15:30:00"
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // POST /api/rides/{id}/cancel - Cancel specific ride
    @PostMapping(
            value = "/{id}/cancel",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<StartRideResponse> cancelRide(@PathVariable Long id) {
        StartRideResponse response = new StartRideResponse(
                id,
                RideStatus.CANCELLED,
                "2025-01-18T14:25:00"
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

  // GET /api/rides/{id}/location - Get current ride location and estimated arrival time
    @GetMapping(
            value = "/{id}/location",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<RideLocationResponse> getRideLocation(@PathVariable Long id) {
      RideLocationResponse response = new RideLocationResponse(
              id,
              45.2671,
              19.8335,
              "Bulevar oslobođenja 46",
              5,
              LocalDateTime.now()
      );
      return ResponseEntity.ok(response);
    }

    // POST /api/rides/{id}/report-inconsistency - Report driver inconsistency
    @PostMapping(
            value = "/{id}/report-inconsistency",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ReportInconsistencyResponse> reportInconsistency(
            @PathVariable Long id,
            @RequestBody ReportInconsistencyRequest request
    ) {
      ReportInconsistencyResponse response = new ReportInconsistencyResponse(
              1L,
              id,
              "Inconsistency report submitted successfully",
              LocalDateTime.now()
      );
      return ResponseEntity.ok(response);
    }

    // DELETE /api/rides/{id} - Delete ride (admin only)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteRide(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }
}
