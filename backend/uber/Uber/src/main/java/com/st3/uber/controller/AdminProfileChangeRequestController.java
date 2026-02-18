package com.st3.uber.controller;


import com.st3.uber.dto.user.admin.AdminDriverProfileChangeRequestDetailsDto;
import com.st3.uber.dto.user.admin.AdminDriverProfileChangeRequestDto;
import com.st3.uber.dto.user.admin.AdminProfileChangeDecisionDto;
import com.st3.uber.service.DriverProfileChangeRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/profile-change-requests")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminProfileChangeRequestController {

    private final DriverProfileChangeRequestService service;

    public AdminProfileChangeRequestController(
            DriverProfileChangeRequestService service
    ) {
        this.service = service;
    }

    @GetMapping("/pending")
    public List<AdminDriverProfileChangeRequestDto> getPendingRequests() {
        return service.getPendingRequests();
    }

    @GetMapping
    public List<AdminDriverProfileChangeRequestDto> getAllRequests() {
        return service.getAllRequests();
    }

    @GetMapping("/{id}")
    public AdminDriverProfileChangeRequestDetailsDto getRequestDetails(
            @PathVariable Long id
    ) {
        return service.getRequestDetails(id);
    }

    @PostMapping("/{id}/decision")
    public ResponseEntity<Void> decideRequest(
            @PathVariable Long id,
            @RequestBody @Valid AdminProfileChangeDecisionDto decision
    ) {
        service.decideRequest(id, decision);
        return ResponseEntity.ok().build();
    }


}