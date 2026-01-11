package com.st3.uber.domain;

import com.st3.uber.enums.RequestChangeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_changes")
@Getter
@Setter
public class RequestChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    private String name;
    private String surname;
    private String phoneNumber;
    private String address;
    private String profileImage;


    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    private String model;
    private String registrationNumber;
    private Integer seatingCapacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestChangeStatus status = RequestChangeStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    private LocalDateTime decidedAt;
}
