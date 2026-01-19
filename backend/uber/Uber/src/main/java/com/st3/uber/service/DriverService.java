package com.st3.uber.service;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Location;
import com.st3.uber.domain.Ride;
import com.st3.uber.enums.RideRejectReason;
import com.st3.uber.exception.RideRejectedException;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.util.DistanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public List<Driver> findAvailableDrivers() {
        return driverRepository.findAvailableDrivers();
    }

    public Driver findDriverForRide(Ride ride) {

        List<Driver> drivers = driverRepository.findAvailableDrivers();

        if (drivers.isEmpty()) {
            throw new RideRejectedException(RideRejectReason.NO_ACTIVE_DRIVERS);
        }

        List<Driver> eligibleDrivers = drivers.stream()
                .filter(d -> d.getVehicle().getType() == ride.getVehicleType())
                .filter(d -> !ride.isBabyTransport() || d.getVehicle().isBabyTransport())
                .filter(d -> !ride.isPetTransport() || d.getVehicle().isPetTransport())
                .filter(d -> d.getWorkingMinutesPerDay() < 480)
                .toList();

        if (eligibleDrivers.isEmpty()) {
            throw new RideRejectedException(RideRejectReason.NO_MATCHING_DRIVERS);
        }

        List<Driver> freeDrivers = eligibleDrivers.stream()
                .filter(Driver::isFree)
                .toList();

        List<Driver> almostFreeDrivers = eligibleDrivers.stream()
                .filter(d -> !d.isFree())
                .filter(d -> d.getCurrentRide() != null)
                .filter(d -> d.getCurrentRide().getRemainingMinutes() <= 10)
                .toList();

        List<Driver> candidates;

        if (!freeDrivers.isEmpty()) {
            candidates = freeDrivers;
        } else if (!almostFreeDrivers.isEmpty()) {
            candidates = almostFreeDrivers;
        } else {
            throw new RideRejectedException(RideRejectReason.NO_ACTIVE_DRIVERS);
        }

        Location pickup = ride.getStartLocation();

        return candidates.stream()
                .filter(d ->
                        d.getCurrentLocation() != null &&
                                d.getCurrentLocation().getLat() != null &&
                                d.getCurrentLocation().getLng() != null
                )
                .min(Comparator.comparingDouble(d ->
                        DistanceCalculator.distanceKm(
                                pickup.getLat(),
                                pickup.getLng(),
                                d.getCurrentLocation().getLat(),
                                d.getCurrentLocation().getLng()
                        )
                ))
                .orElseThrow(() ->
                        new RideRejectedException(RideRejectReason.NO_DRIVER_WITH_LOCATION)
                );
    }

    public void logoutDriver(Long driverId){
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

        if (driver.getCurrentRide() != null) {
            throw new ResponseStatusException(FORBIDDEN, "Driver cannot logout while in a ride");
        }

        driver.setActive(false);
        driver.setAvailable(false);
        driver.setFree(false);
        driverRepository.save(driver);
    }

  public void changeActiveStatus(Long driverId){
      Driver driver = driverRepository.findById(driverId)
          .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Driver not found"));

    if(driver.getNextRides().isEmpty()){
        throw new ResponseStatusException(FORBIDDEN, "Driver cannot change status because of next rides");
    }
    if(driver.getCurrentRide() != null){
      driver.setActivityRequest(true);
      driverRepository.save(driver);
      return;
    }

      boolean newActive = !driver.isActive();
      driver.setActive(newActive);
      driver.setAvailable(newActive);
      driver.setFree(newActive);

      driverRepository.save(driver);
  }

}
