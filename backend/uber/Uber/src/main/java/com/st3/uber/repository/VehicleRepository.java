package com.st3.uber.repository;

import com.st3.uber.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    boolean existsByRegistrationNumber(String registrationNumber);
}
