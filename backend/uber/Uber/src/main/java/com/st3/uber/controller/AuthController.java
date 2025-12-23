package com.st3.uber.controller;

import com.st3.uber.domain.Passenger;
import com.st3.uber.dto.auth.*;
import com.st3.uber.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final UserService userService;

  public AuthController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/register")
  public Passenger registerPassenger(@RequestBody RegisterPassengerRequest req) {
    return userService.createPassenger(req);
  }

  @PostMapping("/login")
  public LoginResponse login(@RequestBody LoginRequest req) {
    return userService.login(req);
  }

  @GetMapping("/hello")
  public String hello() {
    return "Hello, world!";
  }


    // POST /api/auth/email-validation - Validate email availability
    @PostMapping("/email-validation")
    public EmailValidationResponse validateEmail(@RequestBody EmailValidationRequest request) {
        // Dummy logika - proverava samo format
        boolean isValidFormat = request.email() != null
                && request.email().contains("@")
                && request.email().contains(".");

        // Validacija važi 24h
        LocalDateTime validUntil = LocalDateTime.now().plusHours(24);

        String message = isValidFormat
                ? "Email is available and validation is valid for 24 hours"
                : "Invalid email format";

        return new EmailValidationResponse(
                request.email(),
                isValidFormat,
                validUntil,
                message
        );
    }
}
