package com.st3.uber.repository;

import com.st3.uber.domain.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideRepository extends JpaRepository<Ride, Long> {
}
