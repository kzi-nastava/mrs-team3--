package com.st3.uber.controller;

import com.st3.uber.dto.user.UpdateUserProfileRequest;
import com.st3.uber.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:4200")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
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
}
