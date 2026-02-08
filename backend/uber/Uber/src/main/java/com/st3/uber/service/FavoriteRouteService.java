package com.st3.uber.service;

import com.st3.uber.domain.FavoriteRoute;
import com.st3.uber.domain.Location;
import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.dto.location.LocationResponse;
import com.st3.uber.dto.route.FavoriteRouteRequest;
import com.st3.uber.dto.route.FavoriteRouteResponse;
import com.st3.uber.repository.FavoriteRouteRepository;
import com.st3.uber.repository.PassengerRepository;
import com.st3.uber.repository.RideRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class FavoriteRouteService {

    private final FavoriteRouteRepository repository;
    private final PassengerRepository passengerRepository;
    private final RideRepository rideRepository;

    public FavoriteRouteService(
        FavoriteRouteRepository repository,
        PassengerRepository passengerRepository, RideRepository rideRepository) {
        this.repository = repository;
        this.passengerRepository = passengerRepository;
        this.rideRepository = rideRepository;
    }

    @Transactional
    public void add(Long passengerId, FavoriteRouteRequest req) {

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        Ride ride = rideRepository.findById(req.rideId())
            .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        var existing = repository.findByRideIdAndPassenger(req.rideId(), passenger);
        if (existing.isPresent()) {
            return;
        }

        List<Ride> favoriteRides = passenger.getFavoriteRides();
        if (!favoriteRides.contains(ride)) {
            favoriteRides.add(ride);
            passengerRepository.save(passenger);
        }

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

        var stops = req.stops() == null ? List.<Location>of()
            : req.stops().stream()
            .map(s -> new Location(s.latitude(), s.longitude(), s.address()))
            .toList();
        route.setStops(stops);

        route.setVehicleType(req.vehicleType());
        route.setBabyTransport(req.babyTransport());
        route.setPetTransport(req.petTransport());

        route.setRideId(req.rideId());

        repository.save(route);
    }

    @Transactional
    public void remove(Long passengerId, FavoriteRouteRequest req) {

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        Ride ride = rideRepository.findById(req.rideId()).orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        var optRoute = repository.findByRideIdAndPassenger(req.rideId(), passenger);
        optRoute.ifPresent(repository::delete);


        List<Ride> favoriteRides = passenger.getFavoriteRides();
        favoriteRides.remove(ride);
        passengerRepository.save(passenger);

    }

    public List<FavoriteRouteResponse> getAll(Long passengerId) {

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        List<FavoriteRoute> routes = repository.findByPassenger(passenger);

        Map<Long, FavoriteRoute> routesByRideId = routes.stream()
                .collect(Collectors.toMap(
                        FavoriteRoute::getRideId,
                        r -> r,
                        (a, b) -> a
                ));

        for (Ride ride : passenger.getFavoriteRides()) {

            if (!routesByRideId.containsKey(ride.getId())) {

                FavoriteRoute fr = new FavoriteRoute();
                fr.setPassenger(passenger);
                fr.setRideId(ride.getId());

                fr.setStartLocation(new Location(
                        ride.getStartLocation().getLat(),
                        ride.getStartLocation().getLng(),
                        ride.getStartLocation().getAddress()
                ));


                fr.setEndLocation(new Location(
                        ride.getEndLocation().getLat(),
                        ride.getEndLocation().getLng(),
                        ride.getEndLocation().getAddress()
                ));

                List<Location> stops = ride.getRideStops() == null
                        ? List.of()
                        : ride.getRideStops().stream()
                        .map(s -> new Location(
                                s.getLat(),
                                s.getLng(),
                                s.getAddress()
                        ))
                        .toList();

                fr.setStops(stops);

                fr.setVehicleType(ride.getVehicleType());
                fr.setBabyTransport(ride.isBabyTransport());
                fr.setPetTransport(ride.isPetTransport());


                repository.save(fr);

                routes.add(fr);
                routesByRideId.put(ride.getId(), fr);
            }
        }

        return routes.stream()
                .map(r -> {

                    List<Location> stops = r.getStops() == null
                            ? List.of()
                            : r.getStops();

                    return new FavoriteRouteResponse(
                            r.getId(),
                            r.getRideId(),
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
                            stops.stream()
                                    .map(s -> new LocationResponse(
                                            s.getLat(),
                                            s.getLng(),
                                            s.getAddress()
                                    ))
                                    .toList(),
                            r.getVehicleType(),
                            r.isBabyTransport(),
                            r.isPetTransport()
                    );
                })
                .toList();
    }


    @Transactional
    public void addByEmail(String email, FavoriteRouteRequest req) {
        Passenger passenger = passengerRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        add(passenger.getId(), req);
    }

    @Transactional
    public void removeByEmail(String email, FavoriteRouteRequest req) {
        Passenger passenger = passengerRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        remove(passenger.getId(), req);
    }

    @Transactional
    public List<FavoriteRouteResponse> getAllByEmail(String email) {
        Passenger passenger = passengerRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));

        return getAll(passenger.getId());
    }

}
