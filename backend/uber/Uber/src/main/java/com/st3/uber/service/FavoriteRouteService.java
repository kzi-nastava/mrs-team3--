package com.st3.uber.service;

import com.st3.uber.domain.FavoriteRoute;
import com.st3.uber.domain.Location;
import com.st3.uber.domain.Passenger;
import com.st3.uber.dto.location.LocationResponse;
import com.st3.uber.dto.route.FavoriteRouteRequest;
import com.st3.uber.dto.route.FavoriteRouteResponse;
import com.st3.uber.repository.FavoriteRouteRepository;
import com.st3.uber.repository.PassengerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FavoriteRouteService {

    private final FavoriteRouteRepository repository;
    private final PassengerRepository passengerRepository;

    public FavoriteRouteService(
            FavoriteRouteRepository repository,
            PassengerRepository passengerRepository
    ) {
        this.repository = repository;
        this.passengerRepository = passengerRepository;
    }

    // =========================
    // ➕ ADD FAVORITE
    // =========================
    public void add(Long passengerId, FavoriteRouteRequest req) {

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        // spreči duplikate (isti from → to po ADRESI)
        repository.findByPassengerAndStartLocation_AddressAndEndLocation_Address(
                passenger,
                req.from().address(),
                req.to().address()
        ).ifPresent(r -> {
            throw new IllegalStateException("Route already in favorites");
        });

        FavoriteRoute route = new FavoriteRoute();
        route.setPassenger(passenger);

        route.setStartLocation(
                new Location(
                        req.from().latitude(),
                        req.from().longitude(),
                        req.from().address()
                )
        );

        route.setEndLocation(
                new Location(
                        req.to().latitude(),
                        req.to().longitude(),
                        req.to().address()
                )
        );

        route.setStops(
                req.stops().stream()
                        .map(s -> new Location(
                                s.latitude(),
                                s.longitude(),
                                s.address()
                        ))
                        .toList()
        );

        route.setVehicleType(req.vehicleType());
        route.setBabyTransport(req.babyTransport());
        route.setPetTransport(req.petTransport());

        repository.save(route);
    }

    // =========================
    // ➖ REMOVE FAVORITE
    // =========================
    public void remove(Long passengerId, FavoriteRouteRequest req) {

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        FavoriteRoute route = repository
                .findByPassengerAndStartLocation_AddressAndEndLocation_Address(
                        passenger,
                        req.from().address(),
                        req.to().address()
                )
                .orElseThrow(() -> new IllegalArgumentException("Favorite route not found"));

        repository.delete(route);
    }

    // =========================
    // 📥 GET ALL FAVORITES
    // =========================
    public List<FavoriteRouteResponse> getAll(Long passengerId) {

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        return repository.findByPassenger(passenger)
                .stream()
                .map(r -> new FavoriteRouteResponse(
                        r.getId(),
                        new LocationResponse(
                                r.getStartLocation().getLat(),
                                r.getStartLocation().getLng(),
                                r.getStartLocation().getAddress()
                        ),
                        new LocationResponse(
                                r.getEndLocation().getLat(),
                                r.getEndLocation().getLng(),
                                r.getEndLocation().getAddress()
                        ),
                        r.getStops().stream()
                                .map(s -> new LocationResponse(
                                        s.getLat(),
                                        s.getLng(),
                                        s.getAddress()
                                ))
                                .toList(),
                        r.getVehicleType(),
                        r.isBabyTransport(),
                        r.isPetTransport()
                ))
                .toList();
    }


    public void addByEmail(String email, FavoriteRouteRequest req) {
        Passenger passenger = passengerRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        add(passenger.getId(), req);
    }

    public void removeByEmail(String email, FavoriteRouteRequest req) {
        Passenger passenger = passengerRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        remove(passenger.getId(), req);
    }

    public List<FavoriteRouteResponse> getAllByEmail(String email) {
        Passenger passenger = passengerRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        return getAll(passenger.getId());
    }

}
