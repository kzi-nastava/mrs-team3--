package com.st3.uber.repository;

import com.st3.uber.domain.FavoriteRoute;
import com.st3.uber.domain.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRouteRepository
        extends JpaRepository<FavoriteRoute, Long> {

    List<FavoriteRoute> findByPassenger(Passenger passenger);

    Optional<FavoriteRoute> findByPassengerAndStartLocation_AddressAndEndLocation_Address(
            Passenger passenger,
            String startAddress,
            String endAddress
    );
    Optional<FavoriteRoute> findByRideIdAndPassenger(Long id, Passenger passenger);

}
