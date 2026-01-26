package com.st3.uber.controller;

import com.st3.uber.dto.vehicle.ActiveVehicleResponse;
import com.st3.uber.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final DriverService driverService;

    @GetMapping(
            value = "/active",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<ActiveVehicleResponse>> getActiveVehicles() {
        List<ActiveVehicleResponse> vehicles = driverService.getActiveDriverLocations();
        return ResponseEntity.ok(vehicles);
    }


    @GetMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ActiveVehicleResponse> getVehicleById(@PathVariable Long id) {
        // For now, this endpoint is not needed
        return ResponseEntity.notFound().build();
    }
}