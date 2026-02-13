package com.st3.uber.services;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Location;
import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.RideRepository;
import com.st3.uber.service.DriverService;
import com.st3.uber.service.NotificationService;
import com.st3.uber.service.RideService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DriverServiceTest {

  @Mock
  private DriverRepository driverRepository;

  @Mock
  private RideService rideService;

  @Mock
  private RideRepository rideRepository;
  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private DriverService driverService;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  @Test(expectedExceptions = ResponseStatusException.class,
      expectedExceptionsMessageRegExp = "404 NOT_FOUND \"Driver not found\"")
  public void test_finish_ride_when_driver_not_found(){
    when(driverRepository.findById(anyLong())).thenReturn(Optional.empty());

    driverService.finishRide(1L,new Location());

    verifyNoInteractions(rideService);
  }

  @Test(expectedExceptions = ResponseStatusException.class,
      expectedExceptionsMessageRegExp = "400 BAD_REQUEST \"Driver has no active ride\"")
  public void test_finish_ride_when_ride_not_found(){
    Driver driver = createDriverWithActiveRide(null);
    when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));

    driverService.finishRide(1L,new Location());

    verifyNoInteractions(rideService);
  }

  @Test(expectedExceptions = ResponseStatusException.class,
      expectedExceptionsMessageRegExp = "400 BAD_REQUEST \"Can only finish rides that are in progress\"")
  public void test_finish_ride_when_ride_not_in_progress(){
    Ride ride = createActiveRide(new Passenger());
    ride.setStatus(RideStatus.COMPLETED);
    Driver driver = createDriverWithActiveRide(ride);
    when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));

    driverService.finishRide(1L,new Location());

    verifyNoInteractions(rideService);
  }

  private Driver createDriverWithActiveRide(Ride ride) {
    Driver driver = new Driver();

    driver.setId(1L);
    driver.setCurrentRide(ride);
    driver.setAvailable(false);
    driver.setActive(true);
    driver.setFree(false);
    return driver;
  }

  private Ride createActiveRide(Passenger passenger) {
    Ride ride = new Ride();
    ride.setId(1L);
    ride.setStatus(RideStatus.IN_PROGRESS);
    ride.setCreator(passenger);
    return ride;
  }

}
