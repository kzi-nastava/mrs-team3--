package com.st3.uber.domain;

import com.st3.uber.enums.VehicleType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RideWish {
    private boolean petTransport;
    private VehicleType vehicleType;

    private boolean babyTransport;
}
