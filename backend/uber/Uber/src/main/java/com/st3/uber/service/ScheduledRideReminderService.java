package com.st3.uber.service;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.enums.NotificationType;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.RideRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for sending scheduled ride reminders
 * Requirements:
 * - 15 minutes before ride start: Send first reminder
 * - Every 5 minutes after that: Send additional reminders until ride starts
 */
@Service
public class ScheduledRideReminderService {

    private final RideRepository rideRepository;
    private final NotificationService notificationService;
    private final JavaMailSender mailSender;

    // Track which rides have already been notified to avoid duplicates
    private final Set<Long> notifiedRides = new HashSet<>();

    public ScheduledRideReminderService(
            RideRepository rideRepository,
            NotificationService notificationService,
            JavaMailSender mailSender
    ) {
        this.rideRepository = rideRepository;
        this.notificationService = notificationService;
        this.mailSender = mailSender;
    }

    /**
     * Runs every 1 minute to check for upcoming rides
     * Sends reminders at 15, 10, 5 minutes before scheduled time
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds (1 minute)
    @Transactional(readOnly = true)
    public void sendUpcomingRideReminders() {
        LocalDateTime now = LocalDateTime.now();

        // Find all scheduled rides that haven't started yet
        List<Ride> upcomingRides = rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.PENDING);

        for (Ride ride : upcomingRides) {
            if (ride.getScheduledAt() == null) {
                continue;
            }

            long minutesUntilRide = ChronoUnit.MINUTES.between(now, ride.getScheduledAt());

            // Send reminder at 15, 10, and 5 minutes before
            if (shouldSendReminder(ride.getId(), minutesUntilRide)) {
                sendReminderNotifications(ride, minutesUntilRide);
                sendReminderEmails(ride, minutesUntilRide);
                notifiedRides.add(ride.getId());
            }
        }

        // Clean up old entries from notifiedRides to prevent memory leak
        cleanupNotifiedRides();
    }

    /**
     * Determines if a reminder should be sent based on time remaining
     * Send at: 15 minutes, 10 minutes, 5 minutes
     */
    private boolean shouldSendReminder(Long rideId, long minutesUntilRide) {
        // Check if we're at a reminder milestone
        boolean isReminderTime = (minutesUntilRide == 15 || minutesUntilRide == 10 || minutesUntilRide == 5);

        // Only send if we haven't already sent for this specific time
        boolean alreadyNotified = notifiedRides.contains(rideId);

        return isReminderTime && !alreadyNotified;
    }

    /**
     * Send notification reminders to all passengers and driver
     */
    private void sendReminderNotifications(Ride ride, long minutesUntilRide) {
        String passengerMessage = String.format(
                "⏰ Reminder: Your ride is scheduled to start in %d minutes. Pickup at %s",
                minutesUntilRide,
                ride.getStartLocation().getAddress()
        );

        // Notify all passengers
        for (Passenger passenger : ride.getPassengers()) {
            notificationService.createNotification(
                    passenger.getId(),
                    passengerMessage,
                    NotificationType.RIDE_REMINDER,
                    ride.getId()
            );
        }

        // Notify driver if assigned
        if (ride.getDriver() != null) {
            String driverMessage = String.format(
                    "⏰ Reminder: You have a pickup in %d minutes at %s",
                    minutesUntilRide,
                    ride.getStartLocation().getAddress()
            );

            notificationService.createNotification(
                    ride.getDriver().getId(),
                    driverMessage,
                    NotificationType.RIDE_REMINDER,
                    ride.getId()
            );
        }
    }

    /**
     * Send email reminders to all passengers and driver
     */
    private void sendReminderEmails(Ride ride, long minutesUntilRide) {
        // Email to passengers
        for (Passenger passenger : ride.getPassengers()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(passenger.getEmail());
                message.setSubject("Ride Reminder - Starting in " + minutesUntilRide + " minutes");
                message.setText(buildPassengerEmailBody(ride, passenger, minutesUntilRide));
                mailSender.send(message);
            } catch (Exception e) {
                // Log error but don't fail the whole process
                System.err.println("Failed to send email to passenger: " + passenger.getEmail());
            }
        }

        // Email to driver
        if (ride.getDriver() != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(ride.getDriver().getEmail());
                message.setSubject("Pickup Reminder - " + minutesUntilRide + " minutes");
                message.setText(buildDriverEmailBody(ride, minutesUntilRide));
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Failed to send email to driver: " + ride.getDriver().getEmail());
            }
        }
    }

    /**
     * Build email body for passenger
     */
    private String buildPassengerEmailBody(Ride ride, Passenger passenger, long minutesUntilRide) {
        return String.format("""
                Dear %s,
                
                This is a reminder that your ride is scheduled to start in %d minutes.
                
                Ride Details:
                - Pickup Location: %s
                - Destination: %s
                - Scheduled Time: %s
                - Vehicle Type: %s
                - Estimated Cost: %.2f RSD
                
                %s
                
                Please be ready at the pickup location.
                
                Thank you for choosing our service!
                
                Best regards,
                Uber Team
                """,
                passenger.getName(),
                minutesUntilRide,
                ride.getStartLocation().getAddress(),
                ride.getEndLocation().getAddress(),
                ride.getScheduledAt().toString(),
                ride.getVehicleType(),
                ride.getCalculatedPrice(),
                ride.getDriver() != null
                        ? "Driver: " + ride.getDriver().getName() + " " + ride.getDriver().getSurname()
                        : "Driver will be assigned shortly"
        );
    }

    /**
     * Build email body for driver
     */
    private String buildDriverEmailBody(Ride ride, long minutesUntilRide) {
        return String.format("""
                Dear %s,
                
                This is a reminder that you have a pickup in %d minutes.
                
                Ride Details:
                - Pickup Location: %s
                - Destination: %s
                - Scheduled Time: %s
                - Number of Passengers: %d
                - Special Requirements: %s
                
                Please ensure you arrive at the pickup location on time.
                
                Best regards,
                Uber Team
                """,
                ride.getDriver().getName(),
                minutesUntilRide,
                ride.getStartLocation().getAddress(),
                ride.getEndLocation().getAddress(),
                ride.getScheduledAt().toString(),
                ride.getPassengers().size(),
                buildSpecialRequirements(ride)
        );
    }

    /**
     * Build special requirements string
     */
    private String buildSpecialRequirements(Ride ride) {
        StringBuilder requirements = new StringBuilder();
        if (ride.isBabyTransport()) {
            requirements.append("Baby seat required. ");
        }
        if (ride.isPetTransport()) {
            requirements.append("Pet-friendly ride. ");
        }
        if (requirements.length() == 0) {
            requirements.append("None");
        }
        return requirements.toString().trim();
    }

    /**
     * Clean up notified rides that are in the past or have started
     * Run this periodically to prevent memory leak
     */
    private void cleanupNotifiedRides() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        // Find rides that have started or are old
        List<Ride> oldRides = rideRepository.findByScheduledAtBefore(oneHourAgo);

        for (Ride ride : oldRides) {
            notifiedRides.remove(ride.getId());
        }
    }
}