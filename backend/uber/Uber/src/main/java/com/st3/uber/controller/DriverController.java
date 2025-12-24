package com.st3.uber.controller;

import com.st3.uber.dto.register.RegisterDriverRequest;
import com.st3.uber.dto.register.RegisterDriverResponse;
import com.st3.uber.dto.vehicle.VehicleResponse;
import com.st3.uber.enums.VehicleType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }
}
