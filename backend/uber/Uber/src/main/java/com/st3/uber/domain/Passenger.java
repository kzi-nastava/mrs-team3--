package com.st3.uber.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Entity
@DiscriminatorValue("PASSENGER")
public class Passenger extends User {
    @ManyToMany
    @JoinTable(
            name = "passenger_favorite_rides",
            joinColumns = @JoinColumn(name = "passenger_id"),
            inverseJoinColumns = @JoinColumn(name = "ride_id")
    )
    private List<Ride> favoriteRides = new ArrayList<>();

  @Column(nullable = false)
  private boolean verified = false;
}
