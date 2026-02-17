package com.st3.uber.controller;

import com.st3.uber.dto.user.admin.AdminRideTrackingDto;
import com.st3.uber.service.AdminRideService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminRideController {

    private final AdminRideService adminRideService;

    public AdminRideController(AdminRideService adminRideService) {
        this.adminRideService = adminRideService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/drivers/{driverId}/current-ride")
    public ResponseEntity<AdminRideTrackingDto> getDriverCurrentRide(
            @PathVariable Long driverId
    ) {
        AdminRideTrackingDto rideDto = adminRideService.getDriverCurrentRide(driverId);

        if (rideDto == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(rideDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/rides/{rideId}")
    public ResponseEntity<AdminRideTrackingDto> getRideById(
            @PathVariable Long rideId
    ) {
        AdminRideTrackingDto rideDto = adminRideService.getRideById(rideId);

        if (rideDto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(rideDto);
    }
}