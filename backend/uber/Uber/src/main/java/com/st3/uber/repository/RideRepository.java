package com.st3.uber.repository;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {

    boolean existsByCreatorAndStatusIn(Passenger creator, List<RideStatus> statuses);

    List<Ride> findByDriver(Driver driver);

    // Get rides for a driver between two dates
    List<Ride> findByDriverAndStartedAtBetween(Driver driver, LocalDateTime start, LocalDateTime end);

    // Get rides for a driver after a specific date
    List<Ride> findByDriverAndStartedAtAfter(Driver driver, LocalDateTime start);

    // Get rides for a driver before a specific date
    List<Ride> findByDriverAndStartedAtBefore(Driver driver, LocalDateTime end);
}
