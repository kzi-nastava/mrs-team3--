package com.st3.uber.controller;

import com.st3.uber.dto.pricing.PricingChangeRequest;
import com.st3.uber.dto.pricing.PricingResponse;
import com.st3.uber.exception.PricingValidationException;
import com.st3.uber.service.RidePricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final RidePricingService pricingService;


    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PricingResponse> getCurrentPricing() {
        PricingResponse pricing = pricingService.getCurrentPricing();
        return ResponseEntity.ok(pricing);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> updatePricing(@RequestBody PricingChangeRequest request) {
        try {
            PricingResponse updatedPricing = pricingService.updatePricing(request);
            return ResponseEntity.ok(updatedPricing);
        } catch (PricingValidationException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Validation failed");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", "Failed to update pricing configuration");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }


    @GetMapping(value = "/constraints", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getPricingConstraints() {
        Map<String, Object> constraints = new HashMap<>();
        constraints.put("minBasePrice", 50.0);
        constraints.put("maxBasePrice", 10000.0);
        constraints.put("minPricePerKm", 10.0);
        constraints.put("maxPricePerKm", 1000.0);

        return ResponseEntity.ok(constraints);
    }
}