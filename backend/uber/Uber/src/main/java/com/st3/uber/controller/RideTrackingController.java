package com.st3.uber.controller;

import com.st3.uber.domain.Passenger;
import com.st3.uber.dto.ride.*;
import com.st3.uber.repository.UserRepository;
import com.st3.uber.service.RideTrackingService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/ride-tracking")
public class RideTrackingController {

    private final RideTrackingService rideTrackingService;
    private final UserRepository userRepository;

    public RideTrackingController(
            RideTrackingService rideTrackingService,
            UserRepository userRepository
    ) {
        this.rideTrackingService = rideTrackingService;
        this.userRepository = userRepository;
    }

    /**
     * Get current ride for authenticated passenger
     * GET /api/ride-tracking/current
     * Requires: PASSENGER role
     */
    @GetMapping("/current")
    @RolesAllowed("PASSENGER")
    public ResponseEntity<RideTrackingResponse> getCurrentRide(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long passengerId = jwt.getClaim("uid");

        Passenger passenger = userRepository.findById(passengerId)
                .filter(Passenger.class::isInstance)
                .map(Passenger.class::cast)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only passengers can track rides"
                ));

        return rideTrackingService
                .getCurrentRideForPassenger(passenger)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }


    /**
     * Validate tracking token (PUBLIC - for guests)
     * GET /api/ride-tracking/validate/{token}
     */
    @GetMapping("/validate/{token}")
    public ResponseEntity<TrackingTokenValidationResponse> validateToken(
            @PathVariable String token
    ) {
        TrackingTokenValidationResponse response =
                rideTrackingService.validateToken(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Track ride by token (PUBLIC - for guests)
     * GET /api/ride-tracking/token/{token}
     */
    @GetMapping("/token/{token}")
    public ResponseEntity<RideTrackingResponse> trackRideByToken(
            @PathVariable String token
    ) {
        RideTrackingResponse response = rideTrackingService.getRideByToken(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Report inconsistency for authenticated passenger
     * POST /api/ride-tracking/current/report
     * Requires: PASSENGER role
     */
    @PostMapping("/current/report")
    @RolesAllowed("PASSENGER")
    public ResponseEntity<ReportInconsistencyResponse> reportInconsistencyForCurrent(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ReportInconsistencyRequest request
    ) {
        Long passengerId = jwt.getClaim("uid");

        Passenger passenger = userRepository.findById(passengerId)
                .filter(Passenger.class::isInstance)
                .map(Passenger.class::cast)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only passengers can report inconsistencies"
                ));

        ReportInconsistencyResponse response =
                rideTrackingService.reportInconsistencyForCurrentRide(passenger, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    /**
     * Report inconsistency by token (PUBLIC - for guests)
     * POST /api/ride-tracking/token/{token}/report
     */
    @PostMapping("/token/{token}/report")
    public ResponseEntity<ReportInconsistencyResponse> reportInconsistencyByToken(
            @PathVariable String token,
            @RequestBody ReportInconsistencyRequest request
    ) {
        ReportInconsistencyResponse response =
                rideTrackingService.reportInconsistencyByToken(token, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}