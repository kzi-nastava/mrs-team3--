package com.st3.uber.controller;

import com.st3.uber.dto.user.UpdateUserProfileRequest;
import com.st3.uber.exception.PendingProfileChangeRequestException;
import com.st3.uber.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.st3.uber.service.DriverProfileChangeRequestService;
import com.st3.uber.dto.user.driver.DriverProfileChangeRequestDto;
import com.st3.uber.service.ImageStorageService;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:4200")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final DriverProfileChangeRequestService changeRequestService;
    private final ImageStorageService imageStorageService;


    public UserProfileController(UserProfileService userProfileService, DriverProfileChangeRequestService changeRequestService, ImageStorageService imageStorageService) {
        this.userProfileService = userProfileService;
        this.changeRequestService = changeRequestService;
        this.imageStorageService = imageStorageService;
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


    @PostMapping("/me/image")
    public ResponseEntity<String> uploadProfileImage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file
    ) {
        Long userId = jwt.getClaim("uid");
        String imagePath = imageStorageService.saveProfileImage(file, userId);
        String role = jwt.getClaim("role");
        if (!"DRIVER".equals(role)) {
            userProfileService.updateProfileImage(userId, imagePath);
        }
        return ResponseEntity.ok(imagePath);
    }

    @ExceptionHandler(PendingProfileChangeRequestException.class)
    public ResponseEntity<String> handlePendingProfileChange(
            PendingProfileChangeRequestException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409
                .body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        return ResponseEntity
                .badRequest() // 400
                .body(ex.getMessage());
    }


}
