package com.st3.uber.repository;

import com.st3.uber.domain.RideInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RideInviteRepository extends JpaRepository<RideInvite, Long> {

    Optional<RideInvite> findByTrackingToken(String trackingToken);
}
