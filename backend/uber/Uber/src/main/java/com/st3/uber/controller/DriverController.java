package com.st3.uber.controller;

import com.st3.uber.dto.register.RegisterDriverRequest;
import com.st3.uber.dto.register.RegisterDriverResponse;
import com.st3.uber.dto.ride.ReportInconsistencyResponse;
import com.st3.uber.dto.user.driver.DriverActivityRequest;
import com.st3.uber.dto.user.driver.DriverActivityResponse;
import com.st3.uber.dto.user.driver.DriverRideDetailResponse;
import com.st3.uber.dto.user.driver.DriverRideHistoryResponse;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.VehicleType;
import com.st3.uber.service.DriverRegistrationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverRegistrationService driverRegistrationService;

    public DriverController(DriverRegistrationService driverRegistrationService) {
        this.driverRegistrationService = driverRegistrationService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterDriverResponse registerDriver(
            @RequestBody RegisterDriverRequest req
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

    // GET /api/drivers
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RegisterDriverResponse>> getAllDrivers() {

        List<RegisterDriverResponse> drivers = List.of(
                new RegisterDriverResponse(1L, "driver1@test.com", "Marko", "Markovic", null, true),
                new RegisterDriverResponse(2L, "driver2@test.com", "Jovan", "Jovanovic", null, true)
        );

        return ResponseEntity.ok(drivers);
    }

    // GET /api/drivers/{id}/rides
    @GetMapping(
            value = "/{id}/rides",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<DriverRideHistoryResponse>> getDriverRideHistory(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) RideStatus status,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection
    ) {

        List<DriverRideHistoryResponse> rides = List.of(
                new DriverRideHistoryResponse(
                        1L,
                        "Bulevar oslobođenja 46",
                        "Futoška 10",
                        LocalDateTime.now().minusHours(2),
                        LocalDateTime.now().minusHours(1),
                        RideStatus.COMPLETED,
                        450.0,
                        8.5,
                        false,
                        null,
                        false,
                        List.of("John Doe", "Jane Smith")
                )
        );

        return ResponseEntity.ok(rides);
    }

    // GET /api/drivers/{driverId}/rides/{rideId}
    @GetMapping(
            value = "/{driverId}/rides/{rideId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DriverRideDetailResponse> getDriverRideDetail(
            @PathVariable Long driverId,
            @PathVariable Long rideId
    ) {

        DriverRideDetailResponse response = new DriverRideDetailResponse(
                rideId,
                "Bulevar oslobođenja 46",
                "Futoška 10",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                RideStatus.COMPLETED,
                450.0,
                8.5,
                VehicleType.STANDARD,
                false,
                null,
                null,
                false,
                List.of("John Doe", "Jane Smith"),
                List.of(),
                List.of(),
                5,
                4,
                "Great ride!",
                List.of(
                        new ReportInconsistencyResponse(
                                1L,
                                2L,
                                "Driver took a longer route",
                                LocalDateTime.now().minusHours(1).minusMinutes(30)
                        )
                )
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
}
