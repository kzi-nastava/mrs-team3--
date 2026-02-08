package com.st3.uber.repository;

import com.st3.uber.domain.PanicEvent;
import com.st3.uber.domain.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PanicEventRepository extends JpaRepository<PanicEvent, Long> {
  PanicEvent findByRide(Ride ride);
  PanicEvent findByRideAndEmail(Ride ride, String email);
}
