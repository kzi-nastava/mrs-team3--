package com.st3.uber.repository;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.VehicleType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {

    boolean existsByCreatorAndStatusIn(Passenger creator, List<RideStatus> statuses);

    List<Ride> findByDriver(Driver driver);

    // Get rides for a driver between two dates
    List<Ride> findByDriverAndStartedAtBetween(Driver driver, LocalDateTime start, LocalDateTime end);

    // Get rides for a driver after a specific date
    List<Ride> findByDriverAndStartedAtAfter(Driver driver, LocalDateTime start);

    // Get rides for a driver before a specific date
    List<Ride> findByDriverAndStartedAtBefore(Driver driver, LocalDateTime end);

    List<Ride> findByStatusAndScheduledAtIsNotNull(RideStatus status);

    List<Ride> findByScheduledAtBefore(LocalDateTime dateTime);
    List<Ride> findByCreatorAndFinishedAtIsNotNull(Passenger creator);

    List<Ride> findByStatusAndScheduledAtBetween(
            RideStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime
    );


    @Query("SELECT r FROM Ride r WHERE r.status = :status " +
            "AND r.scheduledAt IS NOT NULL " +
            "AND r.scheduledAt BETWEEN :now AND :futureTime")
    List<Ride> findUpcomingScheduledRides(
            @Param("status") RideStatus status,
            @Param("now") LocalDateTime now,
            @Param("futureTime") LocalDateTime futureTime
    );

    @Query("SELECT r FROM Ride r " +
            "LEFT JOIN FETCH r.passengers " +
            "LEFT JOIN FETCH r.driver " +
            "WHERE r.status = :status AND r.scheduledAt IS NOT NULL")
    List<Ride> findPendingRidesWithPassengersAndDriver(@Param("status") RideStatus status);

    List<Ride> findPastByCreator(Passenger passenger);

    // For ride tracking - find active rides for a passenger
    @Query("SELECT r FROM Ride r WHERE :passenger MEMBER OF r.passengers AND r.status IN :statuses")
    List<Ride> findByPassengersContainingAndStatusIn(
            @Param("passenger") Passenger passenger,
            @Param("statuses") List<RideStatus> statuses
    );
    /**
     * Find pending rides that match driver's vehicle capabilities
     * and have no driver assigned yet
     */
    @Query("""
        SELECT r FROM Ride r 
        WHERE r.driver IS NULL 
        AND r.status = 'PENDING'
        AND r.vehicleType = :vehicleType
        AND (:requiresBabyTransport = false OR r.babyTransport = false OR :hasBabyTransport = true)
        AND (:requiresPetTransport = false OR r.petTransport = false OR :hasPetTransport = true)
        ORDER BY r.createdAt ASC
    """)
    List<Ride> findPendingRidesForDriver(
            @Param("vehicleType") VehicleType vehicleType,
            @Param("hasBabyTransport") boolean hasBabyTransport,
            @Param("hasPetTransport") boolean hasPetTransport,
            @Param("requiresBabyTransport") boolean requiresBabyTransport,
            @Param("requiresPetTransport") boolean requiresPetTransport
    );

    @Query("""
    SELECT DISTINCT r FROM Ride r
    LEFT JOIN FETCH r.passengers
    LEFT JOIN FETCH r.driver
    WHERE r.status = :status
    AND r.scheduledAt IS NOT NULL
""")
    List<Ride> findPendingRidesForReminders(@Param("status") RideStatus status);


    /**
     * For finishing rides - with pessimistic lock to prevent concurrent access
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Ride r WHERE r.id = :id")
    Optional<Ride> findByIdWithLock(@Param("id") Long id);

    List<Ride> findByCreatorAndScheduledAtAfterAndStatusIn(
        Passenger creator,
        LocalDateTime dateTime,
        List<RideStatus> statuses
    );

    List<Ride> findByStatusAndScheduledAtBefore(
            RideStatus status,
            LocalDateTime time
    );

    boolean existsByStatusAndScheduledAtBetween(
            RideStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    List<Ride> getAllByStatusIn(List<RideStatus> statuses);
}