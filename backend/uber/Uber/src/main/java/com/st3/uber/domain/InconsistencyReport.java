package com.st3.uber.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inconsistency_reports")
public class InconsistencyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger reportedBy;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(length = 1000, nullable = false)
    private String reportText;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}