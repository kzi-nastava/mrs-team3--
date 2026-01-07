package com.st3.uber.service;

import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.User;
import com.st3.uber.dto.auth.ForgotPasswordRequest;
import com.st3.uber.dto.auth.LoginRequest;
import com.st3.uber.dto.auth.LoginResponse;
import com.st3.uber.dto.auth.RegisterPassengerRequest;
import com.st3.uber.repository.UserRepository;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.EntityResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static java.util.Base64.getDecoder;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest req) {
        User u = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (u.isBlocked()) {
            throw new RuntimeException("User is blocked");
        }

        if (!req.password().equals(u.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (u instanceof Passenger passenger) {
            if (!passenger.isVerified()) {
                throw new RuntimeException("Email not verified");
            }
        }
        String role = u.getClass().getSimpleName().toUpperCase();

        return new LoginResponse(u.getId(), u.getEmail(), role);
    }

    public ResponseEntity<?> forgotPassword(ForgotPasswordRequest req) {
        // TODO generate token and find user by email
        // TODO if fails return EntityResponse with error message



        return new ResponseEntity<>(HttpStatus.OK);
    }
}
