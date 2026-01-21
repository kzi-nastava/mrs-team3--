package com.st3.uber.service;

import com.st3.uber.domain.Ride;
import com.st3.uber.domain.User;
import com.st3.uber.enums.NotificationType;
import com.st3.uber.enums.UserRole;
import com.st3.uber.repository.RideRepository;
import com.st3.uber.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class PanicService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public PanicService(
            RideRepository rideRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void handlePanicEvent(Long rideId, Long userId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        // Mark ride as panic
        ride.setPanic(true);
        rideRepository.save(ride);

        // Get all admins
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);

        // Notify all admins about panic event
        String panicMessage = String.format(
                "⚠️ PANIC ALERT! Ride #%d - Location: %s. Passenger/Driver needs immediate assistance!",
                rideId,
                ride.getStartLocation() != null ? ride.getStartLocation().getAddress() : "Unknown"
        );

        for (User admin : admins) {
            notificationService.createNotification(
                    admin.getId(),
                    panicMessage,
                    NotificationType.PANIC,
                    rideId
            );
        }

        // Also notify the user who pressed panic that help is coming
        notificationService.createNotification(
                userId,
                "Emergency alert sent to support team. Help is on the way!",
                NotificationType.PANIC,
                rideId
        );
    }
}