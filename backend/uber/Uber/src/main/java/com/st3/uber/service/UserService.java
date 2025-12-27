package com.st3.uber.service;

import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.User;
import com.st3.uber.dto.auth.ForgotPasswordRequest;
import com.st3.uber.dto.auth.LoginRequest;
import com.st3.uber.dto.auth.LoginResponse;
import com.st3.uber.dto.auth.RegisterPassengerRequest;
import com.st3.uber.repository.UserRepository;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    @SneakyThrows
    public Passenger createPassenger(RegisterPassengerRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Email already in use");
        }

        Passenger p = new Passenger();
        p.setEmail(req.getEmail());
        p.setPassword(req.getPassword());
        p.setName(req.getName());
        p.setSurname(req.getSurname());
        p.setPhoneNumber(req.getPhoneNumber());
        p.setAddress(req.getAddress());

        if (req.getBase64Image() != null) {
          String fileName = UUID.randomUUID() + "." + req.getExtension();

          p.setImagePath("uploads/" + fileName);

          byte[] imageBytes = getDecoder().decode(req.getBase64Image());
          Files.write(Path.of("uploads/" + fileName), imageBytes);
        }
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
