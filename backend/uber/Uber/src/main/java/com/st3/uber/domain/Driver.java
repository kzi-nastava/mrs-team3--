package com.st3.uber.domain;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "drivers")
@Getter
@Setter
public class Driver extends User {

    @Column(nullable = false)
    private boolean active = false;

    @Column(nullable = false)
    private boolean available = false;

    @Column(nullable = false)
    private boolean free = false;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @OneToMany
    @JoinColumn(name = "driver_id")
    private List<Ride> pastRides = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "current_ride_id")
    private Ride currentRide;

    @Column(nullable = false)
    private int workingMinutesPerDay = 0;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "current_lat")),
            @AttributeOverride(name = "lng", column = @Column(name = "current_lng")),
            @AttributeOverride(name = "address", column = @Column(name = "current_address"))
    })
    private Location currentLocation;

    private LocalDateTime locationUpdatedAt;
}
