package com.st3.uber.domain;

import com.st3.uber.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "ride_pricing")
@Getter
@Setter
public class RidePricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection
    @CollectionTable(
            name = "vehicle_type_prices",
            joinColumns = @JoinColumn(name = "pricing_id")
    )
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "base_price", nullable = false)
    private Map<VehicleType, Double> basePrices = new HashMap<>();

    @Column(nullable = false)
    private double pricePerKm = 120;
}
