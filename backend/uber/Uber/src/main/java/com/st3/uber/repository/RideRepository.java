package com.st3.uber.repository;

import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {

    boolean existsByCreatorAndStatusIn(Passenger creator, List<RideStatus> statuses);

}
