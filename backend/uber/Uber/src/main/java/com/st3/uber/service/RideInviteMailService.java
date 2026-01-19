package com.st3.uber.service;

import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.domain.RideInvite;
import org.springframework.stereotype.Service;

@Service
public class RideInviteMailService {

    private final MailService mailService;

    public RideInviteMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void sendInvite(
            RideInvite invite,
            Passenger creator,
            Ride ride
    ) {
        String subject = "You are invited to a ride";

        String body =
                "You have been invited to join a ride.\n\n" +
                        "Created by: " + creator.getName() + " " + creator.getSurname() + "\n\n" +
                        "From: " + ride.getStartLocation().getAddress() + "\n" +
                        "To: " + ride.getEndLocation().getAddress() + "\n" +
                        "Distance: " + ride.getDistance() + " km\n" +
                        "Estimated time: " + ride.getEstimatedTimeMinutes() + " minutes\n\n" +
                        "You can track the ride using this link:\n" +
                        "http://localhost:4200/ride-tracking/" + invite.getTrackingToken();

        mailService.sendText(invite.getEmail(), subject, body);
    }
}
