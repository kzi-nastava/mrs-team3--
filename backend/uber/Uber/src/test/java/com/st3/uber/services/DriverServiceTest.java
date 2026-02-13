package com.st3.uber.services;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Location;
import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.dto.route.RouteInfo;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.RideRepository;
import com.st3.uber.service.DriverService;
import com.st3.uber.service.NotificationService;
import com.st3.uber.service.RideService;
import com.st3.uber.service.RouteCalculationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
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
  @Mock
  private RouteCalculationService routeCalculationService;

  @InjectMocks
  private DriverService driverService;

  @BeforeClass
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
    ride.setStartLocation(new Location(45.0, 19.0, "Start"));
    ride.setEndLocation(new Location(45.1, 19.1, "End"));
    return ride;
  }

  @Test(dataProvider = "rideStopsData")
  public void test_finish_ride_with_various_stops(List<Location> rideStops, List<Location> actualRideStopsInitial,
                                                  int expectedActualStopCount) {
    Ride ride = createActiveRide(new Passenger());

    ride.setRideStops(new ArrayList<>(rideStops));
    ride.setActualRideStops(new ArrayList<>(actualRideStopsInitial));

    Driver driver = createDriverWithActiveRide(ride);

    when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
    when(rideRepository.saveAndFlush(ride)).thenAnswer(inv -> inv.getArgument(0));
    when(rideRepository.findByStatusAndScheduledAtIsNotNull(ride.getStatus())).thenReturn(List.of());
    when(routeCalculationService.calculateRoute(any(), any(), any())).thenReturn(createRouteInfo());

    driverService.finishRide(1L, new Location(45.05, 19.05, "Stop 1"));

    assert ride.getActualRideStops().size() == expectedActualStopCount
        : "Expected " + expectedActualStopCount + " actual stops but got " + ride.getActualRideStops().size();
  }

  private RouteInfo createRouteInfo() {
    return new RouteInfo(10.0, 15);
  }
  @DataProvider(name = "rideStopsData")
  public Object[][] rideStopsData() {
    return new Object[][]{
        {
            List.of(new Location(45.05, 19.05, "Stop 1")),
            List.of(),
            1
        },

        {
            List.of(new Location(45.05, 19.05, "Stop 1")),
            List.of(new Location(45.06, 19.06, "Existing")),
            1
        },

        {
            List.of(),
            List.of(),
            0
        }
    };
  }


}
