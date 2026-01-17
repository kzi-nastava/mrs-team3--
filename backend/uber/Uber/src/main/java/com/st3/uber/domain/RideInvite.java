package com.st3.uber.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ride_invites")
@Data
public class RideInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private String trackingToken;

    @Column(nullable = false)
    private LocalDateTime createdAt;


}
