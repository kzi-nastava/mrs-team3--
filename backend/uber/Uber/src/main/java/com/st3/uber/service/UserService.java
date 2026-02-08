package com.st3.uber.service;

import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.domain.User;
import com.st3.uber.dto.auth.ForgotPasswordRequest;
import com.st3.uber.dto.auth.LoginRequest;
import com.st3.uber.dto.auth.LoginResponse;
import com.st3.uber.dto.auth.RegisterPassengerRequest;
import com.st3.uber.dto.user.BlockUserRequest;
import com.st3.uber.dto.user.UserDto;
import com.st3.uber.dto.user.admin.ActiveDriverDto;
import com.st3.uber.dto.user.admin.AdminUserDetailsDto;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.RideRepository;
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
import java.util.List;
import java.util.UUID;

import static java.util.Base64.getDecoder;

@Service
public class UserService {

    UserRepository userRepository;
    private final RideRepository rideRepository;

    public UserService(UserRepository userRepository, RideRepository rideRepository) {
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(u -> new UserDto(
                        u.getId(),
                        u.getName(),
                        u.getSurname()
                ))
                .toList();
    }

    public UserDto blockUser(Long id, BlockUserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        user.setBlocked(request.isBlocked());

        if (request.isBlocked()) {
            user.setBlockReason(request.getReason());
        } else {
            user.setBlockReason(null);
        }

        userRepository.save(user);

        return new UserDto(
                user.getId(),
                user.getName(),
                user.getSurname()
        );
    }


    public List<AdminUserDetailsDto> getAllUsersForAdmin() {
        return userRepository.findAll()
                .stream()
                .map(u -> new AdminUserDetailsDto(
                        u.getId(),
                        u.getName(),
                        u.getSurname(),
                        u.getEmail(),
                        u.getPhoneNumber(),
                        u.getAddress(),
                        u.getRole(),
                        u.isBlocked(),
                        u.getBlockReason(),
                        u.isVerified()
                ))
                .toList();
    }


    public List<ActiveDriverDto> getDriversInProgress() {
        return rideRepository.findByStatus(RideStatus.IN_PROGRESS)
                .stream()
                .map(Ride::getDriver)
                .filter(d -> d != null)
                .distinct()
                .map(d -> new ActiveDriverDto(
                        d.getId(),
                        d.getName(),
                        d.getSurname(),
                        d.getEmail(),
                        d.isBlocked()
                ))
                .toList();
    }


}
