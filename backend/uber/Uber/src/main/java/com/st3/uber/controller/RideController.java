package com.st3.uber.controller;

import com.st3.uber.domain.Location;
import com.st3.uber.dto.ride.*;
import com.st3.uber.enums.CancelledBy;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.util.ComparatorUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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


    // POST /api/rides/{id}/complete-detailed - Complete ride with full details
    @PostMapping(
            value = "/{id}/complete-detailed",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CompleteRideResponse> completeRideDetailed(@PathVariable Long id) {
      CompleteRideResponse response = new CompleteRideResponse(
              id,
              RideStatus.COMPLETED,
              LocalDateTime.now().minusMinutes(15),
              LocalDateTime.now(),
              "Bulevar oslobođenja 46",
              "Futoška 10",
              450.0,
              8.5
      );
      return ResponseEntity.ok(response);
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

    // POST /api/rides/{id}/rating - Submit rating for completed ride
    @PostMapping(
            value = "/{id}/rating",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SubmitRatingResponse> submitRating(
            @PathVariable Long id,
            @RequestBody SubmitRatingRequest request
    ) {
      // Service layer will:
      // 1. Verify ride exists and is COMPLETED
      // 2. Verify passenger is part of this ride
      // 3. Verify ride was completed within last 3 days
      // 4. Verify ride hasn't been rated yet (reviewedAt == null)
      // 5. Update ride: driverRating, vehicleRating, reviewComment, reviewedAt
      // 6. Save and return response

      SubmitRatingResponse response = new SubmitRatingResponse(
              id,
              request.driverRating(),
              request.vehicleRating(),
              request.comment(),
              LocalDateTime.now(),
              "Rating submitted successfully"
      );
      return ResponseEntity.ok(response);
    }

  @PutMapping(
          value = "/{rideId}/cancel-universal",
          produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<CancelRideResponse> cancelRide(
      @PathVariable Long rideId,
      @RequestBody(required = false) CancelRideRequest request)
  {
    if(request.getCancellationReason() == null || request.getCancelledBy() == null) {
      return ResponseEntity.badRequest().build();
    }
    CancelRideResponse response = new CancelRideResponse(request, rideId);

    return ResponseEntity.ok(response);
  }

  @PutMapping(
      value = "/{rideId}/stop",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<StopRideResponse> stopRide(
      @PathVariable Long rideId,
      @RequestBody StopRideRequest request)
  {
    StopRideResponse response = new StopRideResponse(
      rideId,
      LocalDateTime.now(),
      1200.0,
      15.0,
      request
    );

    return ResponseEntity.ok(response);
  }

  @GetMapping
  public ResponseEntity<List<AdminRideDetailsResponse>> getAllRidesForAdmin(
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      LocalDateTime from,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      LocalDateTime to,
      @RequestParam(defaultValue = "rideStartTime")
      String sortBy,
      @RequestParam(defaultValue = "DESC")
      String direction
  ) {

    List<AdminRideDetailsResponse> rides = mockRides();

    rides = rides.stream()
        .filter(r -> {
          LocalDateTime t = r.getRideStartTime();
          boolean okFrom = from == null || !t.isBefore(from);
          boolean okTo = to == null || !t.isAfter(to);
          return okFrom && okTo;
        })
        .sorted(ComparatorUtils.buildComparator(sortBy, direction))
        .toList();

    return ResponseEntity.ok(rides);
  }

//  @GetMapping(
//      value = "/{rideId}",
//      produces = MediaType.APPLICATION_JSON_VALUE)
//  public ResponseEntity<AdminRideDetailsResponse> getRideDetails(@PathVariable Long rideId) {
//
//  }


  private List<AdminRideDetailsResponse> mockRides() {
    return List.of(
        new AdminRideDetailsResponse(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().minusMinutes(20),
            List.of(
                new Location(45.2671, 19.8335, "Bulevar oslobođenja 1"),
                new Location(45.2684, 19.8360, "Futoška ulica"),
                new Location(45.2702, 19.8401, "Trg slobode")
            ),
            new Location(45.2671, 19.8335, "Bulevar oslobođenja 1"),
            new Location(45.2702, 19.8401, "Trg slobode"),
            null,
            15.40,
            false
        ),

        new AdminRideDetailsResponse(
            LocalDateTime.now().minusDays(1).minusHours(2),
            LocalDateTime.now().minusDays(1).minusHours(1),
            List.of(
                new Location(45.2550, 19.8450, "Detelinara"),
                new Location(45.2600, 19.8500, "Limanska pijaca")
            ),
            new Location(45.2550, 19.8450, "Detelinara"),
            new Location(45.2600, 19.8500, "Limanska pijaca"),
            CancelledBy.DRIVER,
            0.0,
            true
        )
    );
  }

  private AdminRideAdvancedDetailsResponse mockAdvancedRide() {

    List<Location> route = List.of(
        new Location(45.2671, 19.8335, "Bulevar oslobođenja 1"),
        new Location(45.2684, 19.8360, "Futoška ulica"),
        new Location(45.2702, 19.8401, "Trg slobode")
    );

    Location startLocation =
        new Location(45.2671, 19.8335, "Bulevar oslobođenja 1");

    Location endLocation =
        new Location(45.2702, 19.8401, "Trg slobode");

    AdminRideDetailsResponse baseDetails =
        new AdminRideDetailsResponse(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().minusMinutes(20),
            route,
            startLocation,
            endLocation,
            null,
            15.40,
            false
        );

    AdminRideAdvancedDetailsResponse.UserDetails driver =
        new AdminRideAdvancedDetailsResponse.UserDetails(
            1L,
            "Marko",
            "Marković",
            "marko@mail.com"
        );

    AdminRideAdvancedDetailsResponse.UserDetails passenger1 =
        new AdminRideAdvancedDetailsResponse.UserDetails(
            2L,
            "Ana",
            "Anić",
            "ana@mail.com"
        );

    AdminRideAdvancedDetailsResponse.UserDetails passenger2 =
        new AdminRideAdvancedDetailsResponse.UserDetails(
            3L,
            "Ivan",
            "Ivanović",
            "ivan@mail.com"
        );

    List<String> inconsistencies = List.of(
        "Driver arrived late",
        "Vehicle did not match description"
    );

    List<Integer> ratings = List.of(5, 4);

    return new AdminRideAdvancedDetailsResponse(
        driver,
        List.of(passenger1, passenger2),
        baseDetails
    );
}


  // DELETE /api/rides/{id} - Delete ride (admin only)
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> deleteRide(@PathVariable Long id) {
      return ResponseEntity.noContent().build();
  }
}
