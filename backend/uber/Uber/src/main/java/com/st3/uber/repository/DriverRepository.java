package com.st3.uber.repository;

import com.st3.uber.domain.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    @Query("""
        SELECT d
        FROM Driver d
        WHERE d.active = true
          AND d.available = true
          AND d.free = true
          AND d.currentRide IS NULL
    """)
    List<Driver> findAvailableDrivers();

    @Query("""
        SELECT d
        FROM Driver d
        WHERE d.active = true
    """)
    List<Driver> findActiveDrivers();


}
