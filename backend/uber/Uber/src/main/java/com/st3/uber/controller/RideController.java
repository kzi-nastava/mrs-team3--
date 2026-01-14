package com.st3.uber.controller;

import com.st3.uber.domain.Location;
import com.st3.uber.domain.Ride;
import com.st3.uber.dto.ride.*;
import com.st3.uber.enums.CancelledBy;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.dto.route.RouteEstimateRequest;
import com.st3.uber.dto.route.RouteEstimateResponse;
import com.st3.uber.service.RideService;
import com.st3.uber.util.ComparatorUtils;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ThreadLocalRandom;
import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }


    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RideResponse> createRide(
            @RequestParam Long passengerId,
            @RequestBody CreateRideRequest request
    ) {

        Ride ride = rideService.createRide(passengerId, request);

        RideResponse response = new RideResponse(
                ride.getId(),
                ride.getStatus(),
                0,
                ride.getCalculatedPrice(),
                ride.getVehicleType()
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =====================================================
    // 2.4.3 PORUČIVANJE IZ OMILJENE RUTE – OSTAVLJENO (MOCK)
    // =====================================================
    @PostMapping(
            value = "/favorites/{favoriteRouteId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RideResponse> createRideFromFavorite(
            @PathVariable Long favoriteRouteId,
            @RequestBody CreateRideFromFavoriteRequest request
    ) {

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

    // =====================================================
    // 2.6.1 POČETAK VOŽNJE – KT2 IMPLEMENTACIJA
    // =====================================================
    @PostMapping(
            value = "/{rideId}/start",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<StartRideResponse> startRide(@PathVariable Long rideId) {

        Ride ride = rideService.startRide(rideId);

        StartRideResponse response = new StartRideResponse(
                ride.getId(),
                ride.getStatus(),
                ride.getStartedAt().toString()
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
    @RolesAllowed("PASSENGER")
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
    @RolesAllowed("PASSENGER")
    public ResponseEntity<SubmitRatingResponse> submitRating(
            @PathVariable Long id,
            @RequestBody SubmitRatingRequest request
    ) {
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
    @RolesAllowed({"PASSENGER", "DRIVER"})
    public ResponseEntity<CancelRideResponse> cancelRide(
            @PathVariable Long rideId,
            @RequestBody(required = false) CancelRideRequest request
    ) {
        if (request == null || request.getCancellationReason() == null || request.getCancelledBy() == null) {
            return ResponseEntity.badRequest().build();
        }

        CancelRideResponse response = new CancelRideResponse(request, rideId);
        return ResponseEntity.ok(response);
    }

    @PutMapping(
            value = "/{rideId}/stop",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RolesAllowed("PASSENGER")
    public ResponseEntity<StopRideResponse> stopRide(
            @PathVariable Long rideId,
            @RequestBody StopRideRequest request
    ) {
        StopRideResponse response = new StopRideResponse(
                rideId,
                LocalDateTime.now(),
                1200.0,
                15.0,
                request
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(
            value = "/admin/rides",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RolesAllowed("ADMIN")
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

    @GetMapping(
            value = "/{rideId}/advanced",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @RolesAllowed("ADMIN")
    public ResponseEntity<AdminRideAdvancedDetailsResponse> getRideDetails(@PathVariable Long rideId) {

        List<AdminRideDetailsResponse> rides = mockRides();

        AdminRideDetailsResponse baseDetails;
        if (rideId == 1L) {
            baseDetails = rides.get(0);
        } else if (rideId == 2L) {
            baseDetails = rides.get(1);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        AdminRideAdvancedDetailsResponse.UserDetails driver =
                new AdminRideAdvancedDetailsResponse.UserDetails(
                        1L, "Marko", "Marković", "marko@mail.com"
                );

        AdminRideAdvancedDetailsResponse.UserDetails passenger1 =
                new AdminRideAdvancedDetailsResponse.UserDetails(
                        2L, "Ana", "Anić", "ana@mail.com"
                );

        AdminRideAdvancedDetailsResponse.UserDetails passenger2 =
                new AdminRideAdvancedDetailsResponse.UserDetails(
                        3L, "Ivan", "Ivanović", "ivan@mail.com"
                );

        AdminRideAdvancedDetailsResponse response =
                new AdminRideAdvancedDetailsResponse(
                        driver,
                        List.of(passenger1, passenger2),
                        baseDetails
                );

        return ResponseEntity.ok(response);
    }

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

    @PostMapping(
            value = "/estimate-route",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<RouteEstimateResponse> estimateRoute(
            @RequestBody RouteEstimateRequest request
    ) {
        int random = ThreadLocalRandom.current().nextInt(0, 16);
        return ResponseEntity.ok(new RouteEstimateResponse(random * 2));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteRide(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }
}
