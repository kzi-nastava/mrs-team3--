package com.st3.uber.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link back to Ride
    @ManyToOne(optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    private Integer driverRating;
    private Integer vehicleRating;

    @Column(length = 1000)
    private String reviewComment;

    private LocalDateTime reviewedAt;
}
