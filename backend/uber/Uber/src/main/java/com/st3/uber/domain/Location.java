package com.st3.uber.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class Location {

    private Double lat;
    private Double lng;

    private String address;
}
