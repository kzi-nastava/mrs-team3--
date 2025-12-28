package com.st3.uber.domain;

import com.st3.uber.enums.CancelledBy;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Passenger creator;

    @ManyToMany
    @JoinTable(
            name = "ride_passengers",
            joinColumns = @JoinColumn(name = "ride_id"),
            inverseJoinColumns = @JoinColumn(name = "passenger_id")
    )
    private List<Passenger> passengers = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "start_lat", nullable = false)),
            @AttributeOverride(name = "lng", column = @Column(name = "start_lng", nullable = false)),
            @AttributeOverride(name = "address", column = @Column(name = "start_address"))
    })
    private Location startLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "end_lat", nullable = false)),
            @AttributeOverride(name = "lng", column = @Column(name = "end_lng", nullable = false)),
            @AttributeOverride(name = "address", column = @Column(name = "end_address"))
    })
    private Location endLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "actual_end_lat")),
            @AttributeOverride(name = "lng", column = @Column(name = "actual_end_lng")),
            @AttributeOverride(name = "address", column = @Column(name = "actual_end_address"))
    })
    private Location actualEndLocation;

    @ElementCollection
    @CollectionTable(name = "ride_stops", joinColumns = @JoinColumn(name = "ride_id"))
    @OrderColumn(name = "stop_order")
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "lat", nullable = false)),
            @AttributeOverride(name = "lng", column = @Column(name = "lng", nullable = false)),
            @AttributeOverride(name = "address", column = @Column(name = "address"))
    })
    private List<Location> rideStops = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "actual_ride_stops", joinColumns = @JoinColumn(name = "ride_id"))
    @OrderColumn(name = "stop_order")
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "lat", nullable = false)),
            @AttributeOverride(name = "lng", column = @Column(name = "lng", nullable = false)),
            @AttributeOverride(name = "address", column = @Column(name = "address"))
    })
    private List<Location> actualRideStops = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private boolean stoppedEarly = false;

    private LocalDateTime stoppedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false)
    private double distance;

    @Column(nullable = false)
    private double basePrice;

    @Column(nullable = false)
    private double calculatedPrice;

    @Enumerated(EnumType.STRING)
    private CancelledBy cancelledBy;

    private LocalDateTime cancelledAt;

    @Column(length = 500)
    private String terminationReason;

    @Column(nullable = false)
    private boolean panic = false;

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PanicEvent> panicEvents = new ArrayList<>();

    private Integer driverRating;
    private Integer vehicleRating;

    @Column(length = 1000)
    private String reviewComment;

    private LocalDateTime reviewedAt;

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InconsistencyReport> inconsistencyReports = new ArrayList<>();
}