package com.st3.uber.controller;

import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.VerificationToken;
import com.st3.uber.dto.auth.*;
import com.st3.uber.enums.VerificationTokenType;
import com.st3.uber.exception.TokenAlreadyUsedException;
import com.st3.uber.exception.TokenException;
import com.st3.uber.exception.TokenExpiredException;
import com.st3.uber.exception.TokenInvalidException;
import com.st3.uber.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;

//@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("api/auth")
public class AuthController {
    private final AuthService authService;
    @Value("${app.frontend.url}")
    private String frontendUrl;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Passenger registerPassenger(@RequestBody RegisterPassengerRequest req) {
        return authService.createPassenger(req);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        return authService.forgotPassword(req);
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

    @GetMapping("/verify")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyToken(token);

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(frontendUrl + "/verification-result?status=success"))
            .build();

    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest req) {
        try{
            authService.resetPassword(req);
            return ResponseEntity.ok().build();
        }catch (TokenException ex) {
            if (ex instanceof TokenExpiredException)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
            else
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token");
        }

    }

    @ExceptionHandler(TokenException.class)
    private ResponseEntity<String> handleTokenException(TokenException ex) {
        String status;
        if(ex instanceof TokenExpiredException)
            status = "expired";
        else if(ex instanceof TokenAlreadyUsedException)
            status = "used";
        else
            status = "invalid";
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/verification-result?status=" + status))
                .build();
    }

    @GetMapping("/reset-password/verify")
    public ResponseEntity<Void> verifyResetPassword(@RequestParam String token) {
        try{
            VerificationToken vt = authService.checkTokenValidity(token);

            if (vt.getTokenType() != VerificationTokenType.PASSWORD_RESET) {
                throw new TokenInvalidException("Invalid token type");
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/reset-password?token=" + token))
                .build();
        }catch (TokenException ex) {
            if (ex instanceof TokenExpiredException) {
                throw new TokenExpiredException("Token expired");
            } else
                throw new TokenInvalidException("Invalid token");
        }

    }



}
