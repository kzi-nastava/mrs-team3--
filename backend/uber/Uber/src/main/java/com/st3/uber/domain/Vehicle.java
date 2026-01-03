package com.st3.uber.domain;

import com.st3.uber.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType type;

    @Column(nullable = false)
    private String registrationNumber;

    @Column(nullable = false)
    private int seatingCapacity;

    @Column(nullable = false)
    private boolean babyTransport;

    @Column(nullable = false)
    private boolean petTransport;
}
