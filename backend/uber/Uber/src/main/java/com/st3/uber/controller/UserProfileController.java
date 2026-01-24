package com.st3.uber.controller;

import com.st3.uber.dto.user.UpdateUserProfileRequest;
import com.st3.uber.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.st3.uber.service.DriverProfileChangeRequestService;
import com.st3.uber.dto.user.driver.DriverProfileChangeRequestDto;


@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:4200")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final DriverProfileChangeRequestService changeRequestService;

    public UserProfileController(UserProfileService userProfileService, DriverProfileChangeRequestService changeRequestService) {
        this.userProfileService = userProfileService;
        this.changeRequestService = changeRequestService;
    }


    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("uid");
        return ResponseEntity.ok(userProfileService.getProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<Void> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateUserProfileRequest request
    ) {
        Long userId = jwt.getClaim("uid");
        userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/change-request")
    public ResponseEntity<Void> submitDriverProfileChangeRequest(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody DriverProfileChangeRequestDto request
    ) {
        Long driverId = jwt.getClaim("uid");
        changeRequestService.submitChangeRequest(driverId, request);
        return ResponseEntity.ok().build();
    }

}
