package com.st3.uber.domain;

import com.st3.uber.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "favorite_routes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"passenger_id", "ride_id"})
)
@Data
public class FavoriteRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ride_id", nullable = false)
    private Long rideId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;


    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "start_lat", nullable = false)),
            @AttributeOverride(name = "lng", column = @Column(name = "start_lng", nullable = false)),
            @AttributeOverride(name = "address", column = @Column(name = "start_address", nullable = false))
    })
    private Location startLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "end_lat", nullable = false)),
            @AttributeOverride(name = "lng", column = @Column(name = "end_lng", nullable = false)),
            @AttributeOverride(name = "address", column = @Column(name = "end_address", nullable = false))
    })
    private Location endLocation;

    @ElementCollection
    @CollectionTable(name = "favorite_route_stops", joinColumns = @JoinColumn(name = "favorite_route_id"))
    @OrderColumn(name = "stop_order")
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "lat", nullable = false)),
            @AttributeOverride(name = "lng", column = @Column(name = "lng", nullable = false)),
            @AttributeOverride(name = "address", column = @Column(name = "address", nullable = false))
    })
    private List<Location> stops = new ArrayList<>();


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false)
    private boolean babyTransport;

    @Column(nullable = false)
    private boolean petTransport;
}
