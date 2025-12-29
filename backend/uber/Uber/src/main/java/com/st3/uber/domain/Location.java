package com.st3.uber.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
public class Location {

    private Double lat;
    private Double lng;

    private String address;

  public Location(double lat, double lng, String address) {
    this.lat = lat;
    this.lng = lng;
    this.address = address;
  }
}
