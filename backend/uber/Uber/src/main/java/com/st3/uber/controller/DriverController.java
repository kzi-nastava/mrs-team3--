package com.st3.uber.controller;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Location;
import com.st3.uber.domain.Ride;
import com.st3.uber.dto.register.RegisterDriverRequest;
import com.st3.uber.dto.register.RegisterDriverResponse;
import com.st3.uber.dto.ride.FinishRideRequest;
import com.st3.uber.dto.ride.FinishRideResponse;
import com.st3.uber.dto.ride.PendingRideResponse;
import com.st3.uber.dto.route.ReachStopRequest;
import com.st3.uber.dto.route.StopStatus;
import com.st3.uber.dto.user.driver.*;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.service.DriverRegistrationService;
import com.st3.uber.service.DriverRideHistoryService;
import com.st3.uber.service.DriverService;
import com.st3.uber.service.RideService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverRegistrationService driverRegistrationService;
    private final DriverRideHistoryService driverRideHistoryService;
    private final DriverService driverService;
    private final RideService rideService;
    private final DriverRepository driverRepository;

    public DriverController(DriverRegistrationService driverRegistrationService, DriverService driverService,
                            DriverRideHistoryService driverRideHistoryService, RideService rideService, DriverRepository driverRepository) {
        this.driverRegistrationService = driverRegistrationService;
        this.driverService = driverService;
        this.driverRideHistoryService = driverRideHistoryService;
        this.rideService = rideService;
        this.driverRepository = driverRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterDriverResponse registerDriver(
            @RequestBody @Valid RegisterDriverRequest req
    ) {
        return driverRegistrationService.register(req);
    }
    // GET /api/drivers/{id}
    @GetMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<RegisterDriverResponse> getDriver(@PathVariable Long id) {
        RegisterDriverResponse response = new RegisterDriverResponse(
                id,
                "driver@test.com",
                "Marko",
                "Markovic",
                null,
                true
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RegisterDriverResponse>> getAllDrivers() {
        List<RegisterDriverResponse> drivers = List.of(
                new RegisterDriverResponse(1L, "driver1@test.com", "Marko", "Markovic", null, true),
                new RegisterDriverResponse(2L, "driver2@test.com", "Jovan", "Jovanovic", null, true)
        );
        return ResponseEntity.ok(drivers);
    }


    // GET api/drivers/{id}/rides
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping(
            value = "/{id}/rides",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<DriverRideHistoryResponse>> getDriverRideHistory(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        List<DriverRideHistoryResponse> rides = driverRideHistoryService.getDriverRideHistory(
                id, startDate, endDate
        );
        return ResponseEntity.ok(rides);
    }

    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping(
            value = "/{driverId}/rides/{rideId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DriverRideDetailResponse> getDriverRideDetail(
            @PathVariable Long driverId,
            @PathVariable Long rideId
    ) {
        DriverRideDetailResponse response = driverRideHistoryService.getDriverRideDetail(
                driverId, rideId
        );
        return ResponseEntity.ok(response);
    }


    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<RegisterDriverResponse> updateDriver(
            @PathVariable Long id,
            @RequestBody RegisterDriverRequest req
    ) {
        RegisterDriverResponse response = new RegisterDriverResponse(
                id,
                req.email(),
                req.firstName(),
                req.lastName(),
                null,
                true
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<DriverActivityResponse> setDriverActivity(
            @PathVariable Long id,
            @RequestBody DriverActivityRequest request
    ) {
        return ResponseEntity.ok(new DriverActivityResponse(id, request.isActive()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/logout")
    public ResponseEntity<Void> logoutDriver(Authentication authentication) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        Long uid = jwt.getToken().getClaim("uid");

        driverService.logoutDriver(uid);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/change-active-status")
    public ResponseEntity<DriverStatusResponse> changeActiveStatus(Authentication authentication) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        Long uid = jwt.getToken().getClaim("uid");

        return ResponseEntity.ok(driverService.changeActiveStatus(uid));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping(
            value = "/all-rides",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<DriverRideResponse>> getMyRides(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long driverId = jwt.getClaim("uid");
        List<Ride> rides = driverService.getDriverRides(driverId);

        List<DriverRideResponse> responses = rides.stream()
                .map(this::mapToDriverRideResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }


    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping(
            value = "/pending-rides",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<PendingRideResponse>> getPendingRides(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long driverId = jwt.getClaim("uid");
        List<Ride> rides = driverService.getPendingRidesForDriver(driverId);

        List<PendingRideResponse> responses = rides.stream()
                .map(this::mapToPendingRideResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping(
            value = "/rides/{rideId}/accept",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DriverRideResponse> acceptRide(
            @PathVariable Long rideId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long driverId = jwt.getClaim("uid");
        Ride ride = driverService.acceptRide(driverId, rideId, rideService);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToDriverRideResponse(ride));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping(
            value = "/rides/{rideId}/start",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DriverRideResponse> startRide(
            @PathVariable Long rideId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long driverId = jwt.getClaim("uid");

        Ride ride = driverService.startRide(driverId, rideId, rideService);

        return ResponseEntity.ok(mapToDriverRideResponse(ride));
    }


    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping(
            value = "/rides/{rideId}/finish",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FinishRideResponse> finishRide(
            @PathVariable Long rideId,
            @RequestBody FinishRideRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long driverId = jwt.getClaim("uid");
        System.out.println("1. " + rideId + driverId);
        Ride ride = driverService.finishRide(driverId, request.actualEndLocation());
        driverService.changeActiveStatusAfterRequest(driverId);

        System.out.println(". " + rideId + " " + driverId + " " + ride.getDriver());

        List<Ride> driverRides = driverService.getDriverRides(driverId);
        boolean hasNextRide = !driverRides.isEmpty();
        Long nextRideId = hasNextRide ? driverRides.get(0).getId() : null;

        FinishRideResponse response = new FinishRideResponse(
                ride.getId(),
                ride.getStatus(),
                ride.getFinishedAt(),
                ride.getCalculatedPrice(),
                hasNextRide,
                nextRideId
        );

        return ResponseEntity.ok(response);
    }


    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping(
            value = "/location",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> updateLocation(
            @RequestBody UpdateLocationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long driverId = jwt.getClaim("uid");
        driverService.updateDriverLocation(driverId, request.latitude(), request.longitude());
        return ResponseEntity.noContent().build();
    }


    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping(value = "/move-to-start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> moveToStart(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long driverId = jwt.getClaim("uid");
        driverService.moveToStartPosition(driverId);
        return ResponseEntity.noContent().build();
    }


    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping(
            value = "/reach-stop",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DriverRideResponse> reachStop(
            @RequestBody ReachStopRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long driverId = jwt.getClaim("uid");
        Ride ride = driverService.reachStop(driverId, request.stopIndex(), rideService);
        return ResponseEntity.ok(mapToDriverRideResponse(ride));
    }


    private DriverRideResponse mapToDriverRideResponse(Ride ride) {
        String creatorName = ride.getCreator() != null
                ? ride.getCreator().getName() + " " + ride.getCreator().getSurname()
                : "Unknown";

        List<StopStatus> stopStatuses = IntStream
                .range(0, ride.getRideStops().size())
                .mapToObj(index -> {
                    var stop = ride.getRideStops().get(index);

                    boolean reached = ride.getActualRideStops().stream()
                            .anyMatch(s -> s.getLat().equals(stop.getLat())
                                    && s.getLng().equals(stop.getLng()));

                    return new StopStatus(index, stop, reached, null);
                })
                .toList();

        Location driverCurrentLocation = null;
        if (ride.getDriver() != null && ride.getDriver().getCurrentLocation() != null) {
            driverCurrentLocation = ride.getDriver().getCurrentLocation();
        }

        return new DriverRideResponse(
                ride.getId(),
                ride.getStatus(),
                ride.getStartLocation(),
                ride.getEndLocation(),
                ride.getRideStops(),
                stopStatuses,
                driverCurrentLocation,
                ride.getEstimatedTimeMinutes(),
                ride.getRemainingMinutes(),
                ride.getScheduledAt(),
                ride.getStartedAt(),
                ride.getDistance(),
                ride.getCalculatedPrice(),
                ride.getVehicleType(),
                ride.isBabyTransport(),
                ride.isPetTransport(),
                ride.getPassengers().size(),
                creatorName
        );
    }

    private PendingRideResponse mapToPendingRideResponse(Ride ride) {
        return new PendingRideResponse(
                ride.getId(),
                ride.getStartLocation(),
                ride.getEndLocation(),
                ride.getRideStops(),
                ride.getEstimatedTimeMinutes(),
                ride.getCreatedAt(),
                ride.getDistance(),
                ride.getCalculatedPrice(),
                ride.getVehicleType(),
                ride.isBabyTransport(),
                ride.isPetTransport(),
                ride.getPassengers().size()
        );
    }

    @PostMapping("/{rideId}/cancel")
    public void cancelRideByDriver(@RequestBody DriverCancelRideRequest reason,
                                   @AuthenticationPrincipal Jwt jwt,
                                   @PathVariable Long rideId) {
        Long driverId = jwt.getClaim("uid");
        driverService.cancelRideByDriver(rideId, driverId, reason);
    }

    @PostMapping("/rides/{rideId}/panic")
    public ResponseEntity<Void> panicRideByDriver(@PathVariable Long rideId,
                                   @AuthenticationPrincipal Jwt jwt) {
        Long driverId = jwt.getClaim("uid");
        driverService.panicRideByDriver(rideId, driverId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<DriverStatusResponse> getStatus(Authentication authentication) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        Long uid = jwt.getToken().getClaim("uid");

        Driver d = driverRepository.findById(uid)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        boolean inRide = d.getCurrentRide() != null;

        return ResponseEntity.ok(new DriverStatusResponse(
            d.isActive(),
            d.isActivityRequest(),
            inRide
        ));
    }

}


