package com.st3.uber.controller;

import com.st3.uber.dto.register.RegisterDriverRequest;
import com.st3.uber.dto.register.RegisterDriverResponse;
import com.st3.uber.dto.user.driver.DriverActivityRequest;
import com.st3.uber.dto.user.driver.DriverActivityResponse;
import com.st3.uber.dto.user.driver.DriverRideDetailResponse;
import com.st3.uber.dto.user.driver.DriverRideHistoryResponse;
import com.st3.uber.service.DriverRegistrationService;
import com.st3.uber.service.DriverRideHistoryService;
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
    private final DriverRideHistoryService driverRideHistoryService;

    public DriverController(
            DriverRegistrationService driverRegistrationService,
            DriverRideHistoryService driverRideHistoryService
    ) {
        this.driverRegistrationService = driverRegistrationService;
        this.driverRideHistoryService = driverRideHistoryService;
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

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RegisterDriverResponse>> getAllDrivers() {
        List<RegisterDriverResponse> drivers = List.of(
                new RegisterDriverResponse(1L, "driver1@test.com", "Marko", "Markovic", null, true),
                new RegisterDriverResponse(2L, "driver2@test.com", "Jovan", "Jovanovic", null, true)
        );
        return ResponseEntity.ok(drivers);
    }

    // ============ RIDE HISTORY ENDPOINTS ============

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

    // ============ OTHER ENDPOINTS ============

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