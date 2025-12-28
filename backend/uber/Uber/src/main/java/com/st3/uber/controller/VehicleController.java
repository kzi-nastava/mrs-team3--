package com.st3.uber.controller;

import com.st3.uber.dto.vehicle.ActiveVehicleResponse;
import com.st3.uber.enums.VehicleType;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    //  - Get all active vehicles with their current positions
    @GetMapping(
            value = "/active",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<ActiveVehicleResponse>> getActiveVehicles() {
        List<ActiveVehicleResponse> vehicles = List.of(
                new ActiveVehicleResponse(
                        1L,
                        45.2671,
                        19.8335,
                        "Bulevar oslobođenja 46",
                        true,
                        VehicleType.STANDARD,
                        "NS-123-AB",
                        LocalDateTime.now()
                ),
                new ActiveVehicleResponse(
                        2L,
                        45.2551,
                        19.8451,
                        "Futoška 10",
                        false,
                        VehicleType.LUXURY,
                        "NS-456-CD",
                        LocalDateTime.now()
                )
        );

        return ResponseEntity.ok(vehicles);
    }

    // Get vehicle by ID
    @GetMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ActiveVehicleResponse> getVehicleById(@PathVariable Long id) {
        ActiveVehicleResponse vehicle = new ActiveVehicleResponse(
                id,
                45.2671,
                19.8335,
                "Bulevar oslobođenja 46",
                true,
                VehicleType.STANDARD,
                "NS-123-AB",
                LocalDateTime.now()
        );

        return ResponseEntity.ok(vehicle);
    }
}