package com.st3.uber.repository;

import com.st3.uber.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Find a review by ride ID and the passenger who left it
     * This ensures each passenger can only have one review per ride
     */
    @Query("SELECT r FROM Review r WHERE r.ride.id = :rideId AND r.passenger.id = :passengerId")
    Optional<Review> findByRideIdAndPassengerId(@Param("rideId") Long rideId,
                                                @Param("passengerId") Long passengerId);

    /**
     * Check if a review already exists for a specific ride and passenger
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Review r WHERE r.ride.id = :rideId AND r.passenger.id = :passengerId")
    boolean existsByRideIdAndPassengerId(@Param("rideId") Long rideId,
                                         @Param("passengerId") Long passengerId);
}