package com.st3.uber.domain;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "passengers")
public class Passenger extends User {
    @ManyToMany
    @JoinTable(
            name = "passenger_favorite_rides",
            joinColumns = @JoinColumn(name = "passenger_id"),
            inverseJoinColumns = @JoinColumn(name = "ride_id")
    )
    private List<Ride> favoriteRides = new ArrayList<>();

}
