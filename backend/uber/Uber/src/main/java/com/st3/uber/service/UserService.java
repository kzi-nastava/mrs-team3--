package com.st3.uber.service;

import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.User;
import com.st3.uber.dto.auth.LoginRequest;
import com.st3.uber.dto.auth.LoginResponse;
import com.st3.uber.dto.auth.RegisterPassengerRequest;
import com.st3.uber.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Passenger createPassenger(RegisterPassengerRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new RuntimeException("Email already exists");
        }

        Passenger p = new Passenger();
        p.setEmail(req.email());
        p.setPassword(req.password());
        p.setName(req.firstName());
        p.setSurname(req.lastName());
        p.setPhoneNumber(req.phoneNumber());
        p.setAddress(req.address());
        p.setBlocked(false);

        return userRepository.save(p);
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

        String role = u.getClass().getSimpleName().toUpperCase();
        return new LoginResponse(u.getId(), u.getEmail(), role);
    }
}
