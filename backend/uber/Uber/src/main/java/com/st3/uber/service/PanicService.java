package com.st3.uber.service;

import com.st3.uber.domain.PanicEvent;
import com.st3.uber.domain.Ride;
import com.st3.uber.domain.RideInvite;
import com.st3.uber.domain.User;
import com.st3.uber.enums.NotificationType;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.UserRole;
import com.st3.uber.repository.PanicEventRepository;
import com.st3.uber.repository.RideInviteRepository;
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
    private final PanicEventRepository panicEventRepository;
    private final RideInviteRepository rideInviteRepository;

    public PanicService(
        RideRepository rideRepository,
        UserRepository userRepository,
        NotificationService notificationService,
        PanicEventRepository panicEventRepository, RideInviteRepository rideInviteRepository) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.panicEventRepository = panicEventRepository;
        this.rideInviteRepository = rideInviteRepository;
    }

    public void handlePanicEvent(Ride ride, User user) {

        List<User> admins = userRepository.findByRole(UserRole.ADMIN);

        String panicMessage = String.format(
                "⚠️ PANIC ALERT! Ride #%d - Location: %s. Passenger/Driver needs immediate assistance!",
                ride.getId(),
                ride.getStartLocation() != null ? ride.getStartLocation().getAddress() : "Unknown"
        );

        for (User admin : admins) {
            notificationService.createNotification(
                    admin.getId(),
                    panicMessage,
                    NotificationType.PANIC,
                    ride.getId()
            );
        }

        notificationService.createNotification(
                user.getId(),
                "Emergency alert sent to support team. Help is on the way!",
                NotificationType.PANIC,
                ride.getId()
        );
    }

    @Transactional
    public void handlePanicPressed(Long rideId, Long id){
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));

        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if(panicEventRepository.findByRideAndEmail(ride, user.getEmail()) != null){
            return;
        }
        savePanic(ride, user);
        handlePanicEvent(ride, user);
    }

    private void savePanic(Ride ride, User user){
        ride.setStatus(RideStatus.PANIC);
        ride.setPanic(true);
        PanicEvent panicEvent = new PanicEvent();
        panicEvent.setRide(ride);
        panicEvent.setTriggeredBy(user);
        panicEvent.setEmail(user.getEmail());
        panicEvent.setCreatedAt(java.time.LocalDateTime.now());
        ride.getPanicEvents().add(panicEvent);

        panicEventRepository.save(panicEvent);
        rideRepository.save(ride);
    }

    @Transactional
    public void handleGuestPanicPressed(String token){
        RideInvite invite = rideInviteRepository.findByTrackingToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid tracking token"));

        Ride ride = invite.getRide();
        if (ride == null) {
            throw new RuntimeException("Ride not found for token");
        }

        String email = invite.getEmail();
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Invite email missing");
        }

        if (panicEventRepository.findByRideAndEmail(ride, email) != null) {
            return;
        }

        PanicEvent panicEvent = new PanicEvent();
        panicEvent.setRide(ride);
        panicEvent.setTriggeredBy(null);
        panicEvent.setEmail(email);
        panicEvent.setCreatedAt(java.time.LocalDateTime.now());

        ride.setStatus(RideStatus.PANIC);
        ride.setPanic(true);

        panicEventRepository.save(panicEvent);
        rideRepository.save(ride);

        handlePanicEvent(ride, ride.getCreator());
    }
}