package com.st3.uber.controller;

import com.st3.uber.dto.register.RegisterDriverRequest;
import com.st3.uber.dto.register.RegisterDriverResponse;
import com.st3.uber.dto.ride.ReportInconsistencyResponse;
import com.st3.uber.dto.user.driver.DriverRideDetailResponse;
import com.st3.uber.dto.user.driver.DriverRideHistoryResponse;
import com.st3.uber.dto.vehicle.VehicleResponse;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.VehicleType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    // POST /api/drivers - Create new driver
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )


    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RegisterDriverResponse> registerDriver(@RequestBody RegisterDriverRequest req ) {
        VehicleResponse vehicleResponse = new VehicleResponse(
          10L,
          req.request().model(),
          req.request().type() != null ? req.request().type() : VehicleType.STANDARD
        );

        RegisterDriverResponse response = new RegisterDriverResponse(
          1L,
          req.email(),
          req.firstName(),
          req.lastName(),
          vehicleResponse,
          false
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    // GET /api/drivers/{id} - Get specific driver
    @GetMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<RegisterDriverResponse> getDriver(@PathVariable Long id) {
        return ResponseEntity.ok().build();
    }

    // GET /api/drivers - Get all drivers
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllDrivers() {
        return ResponseEntity.ok().build();
    }

    // PUT /api/drivers/{id} - Update driver
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<RegisterDriverResponse> updateDriver(
            @PathVariable Long id,
            @RequestBody RegisterDriverRequest req
    ) {
        return ResponseEntity.ok().build();
    }


    // GET /api/drivers/{id}/rides - Get driver's ride history with filtering and sorting
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
        // Service layer will:
        // 1. Get driver by id
        // 2. Filter rides by date range (startDate, endDate)
        // 3. Filter by status if provided
        // 4. Sort by specified field and direction
        // 5. Map to DriverRideHistoryResponse

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
                ),
                new DriverRideHistoryResponse(
                        2L,
                        "Futoška 10",
                        "Bulevar Cara Lazara 25",
                        LocalDateTime.now().minusHours(5),
                        LocalDateTime.now().minusHours(4),
                        RideStatus.COMPLETED,
                        320.0,
                        6.2,
                        false,
                        null,
                        false,
                        List.of("Mike Johnson")
                )
        );

        return ResponseEntity.ok(rides);
    }

    // GET /api/drivers/{driverId}/rides/{rideId} - Get detailed information about specific ride
    @GetMapping(
            value = "/{driverId}/rides/{rideId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DriverRideDetailResponse> getDriverRideDetail(
            @PathVariable Long driverId,
            @PathVariable Long rideId
    ) {
        // Service layer will:
        // 1. Get driver by driverId
        // 2. Get ride by rideId and verify it belongs to this driver
        // 3. Load all related data (passengers, stops, reports, ratings)
        // 4. Map to DriverRideDetailResponse

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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }
}
