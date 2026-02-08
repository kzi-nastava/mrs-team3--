package com.st3.uber.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"ride_id", "passenger_id"})
        })
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link back to Ride
    @ManyToOne(optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    // Link to the passenger who left the review
    @ManyToOne(optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @Column(nullable = false)
    private Integer driverRating;

    @Column(nullable = false)
    private Integer vehicleRating;

    @Column(length = 1000)
    private String reviewComment;

    @Column(nullable = false)
    private LocalDateTime reviewedAt;
}