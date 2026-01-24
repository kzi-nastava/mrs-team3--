package com.st3.uber.service;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.User;
import com.st3.uber.domain.Vehicle;
import com.st3.uber.dto.user.UpdateUserProfileRequest;
import com.st3.uber.dto.user.admin.AdminProfileResponse;
import com.st3.uber.dto.user.driver.DriverProfileResponse;
import com.st3.uber.dto.user.passenger.PassengerProfileResponse;
import com.st3.uber.dto.vehicle.VehicleResponse;
import com.st3.uber.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Object getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user instanceof Driver driver) {

            Vehicle v = driver.getVehicle();

            VehicleResponse vehicleResponse = new VehicleResponse(
                    v.getId(),
                    v.getModel(),
                    v.getType(),
                    v.getRegistrationNumber(),
                    v.getSeatingCapacity(),
                    v.isBabyTransport(),
                    v.isPetTransport()
            );

            return new DriverProfileResponse(
                    driver.getId(),
                    driver.getEmail(),
                    driver.getName(),
                    driver.getSurname(),
                    driver.getPhoneNumber(),
                    driver.getAddress(),
                    vehicleResponse,
                    driver.isActive()
            );
        }

        if (user instanceof Passenger passenger) {
            return new PassengerProfileResponse(
                    passenger.getId(),
                    passenger.getEmail(),
                    passenger.getName(),
                    passenger.getSurname(),
                    passenger.getPhoneNumber(),
                    passenger.getAddress()
            );
        }

        return new AdminProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getSurname(),
                user.getPhoneNumber(),
                user.getAddress()
        );
    }

    @Transactional
    public void updateProfile(Long userId, UpdateUserProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user instanceof Passenger || !(user instanceof Driver)) {
            user.setName(req.firstName());
            user.setSurname(req.lastName());
            user.setPhoneNumber(req.phoneNumber());
            user.setAddress(req.address());

            userRepository.save(user);
        }
    }
}