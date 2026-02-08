package com.st3.uber.domain;

import com.st3.uber.enums.ChangeRequestStatus;
import com.st3.uber.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "driver_profile_change_requests")
public class DriverProfileChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Veza ka vozaču
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    // ===== User (novi podaci) =====
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String profileImage;

    // ===== Vehicle (novi podaci) =====
    private String vehicleModel;
    private String vehicleRegistrationNumber;
    private Integer vehicleSeatingCapacity;

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    private Boolean babyTransport;
    private Boolean petTransport;

    // ===== Status =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeRequestStatus status;

    // ===== Audit =====
    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime reviewedAt;

    private String rejectionReason;
}
