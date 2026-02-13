package com.st3.uber.services;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Location;
import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.dto.route.RouteInfo;
import com.st3.uber.enums.NotificationType;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.RideRepository;
import com.st3.uber.service.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class DriverServiceTest {

  @Mock
  private DriverRepository driverRepository;
  @Mock
  private MailService mailService;
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

  @BeforeMethod
  public void resetMocks() {
    reset(driverRepository, rideRepository, routeCalculationService, notificationService, rideService, mailService);
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
    ride.setPassengers(new ArrayList<>());
    ride.setInvites(new ArrayList<>());
    ride.setRideStops(new ArrayList<>());
    ride.setActualRideStops(new ArrayList<>());

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
    when(rideRepository.saveAndFlush(ride)).thenReturn(ride);
    when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.PENDING)).thenReturn(List.of());
    when(routeCalculationService.calculateRoute(any(), any(), any())).thenReturn(createRouteInfo());
    when(driverRepository.save(driver)).thenReturn(driver);

    driverService.finishRide(1L, new Location(45.05, 19.05, "Stop 1"));

    assertEquals(ride.getActualRideStops().size(), expectedActualStopCount,
        "Expected " + expectedActualStopCount + " actual stops but got " + ride.getActualRideStops().size());
    verify(rideRepository).saveAndFlush(ride);
    verify(driverRepository).save(driver);
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

  @Test
  public void test_finish_ride_when_completed(){
    Location endLocation = new Location(80.05, 100.05, "End");
    Ride ride = createActiveRide(new Passenger());
    ride.setEndLocation(endLocation);
    Driver driver = createDriverWithActiveRide(ride);

    when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
    when(rideRepository.saveAndFlush(ride)).thenReturn(ride);
    when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.PENDING)).thenReturn(List.of());
    when(driverRepository.save(driver)).thenReturn(driver);

    driverService.finishRide(1L, endLocation);

    assertEquals(ride.getStatus(), RideStatus.COMPLETED, "Expected ride status to be COMPLETED but got " + ride.getStatus());
    verify(rideRepository).saveAndFlush(ride);
    verify(driverRepository).save(driver);
  }

  @Test
  public void test_finish_ride_when_finished_early() {
    Location stopLocation = new Location(45.05, 19.05, "Stop 1");
    Location endLocation = new Location(48.1, 19.1, "End");

    Ride ride = createActiveRide(new Passenger());
    ride.setEndLocation(endLocation);
    Driver driver = createDriverWithActiveRide(ride);

    when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
    when(rideRepository.saveAndFlush(ride)).thenReturn(ride);
    when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.PENDING)).thenReturn(List.of());
    when(routeCalculationService.calculateRoute(any(), any(), any())).thenReturn(createRouteInfo());
    when(driverRepository.save(driver)).thenReturn(driver);

    driverService.finishRide(1L, stopLocation);

    assertEquals(ride.getStatus(), RideStatus.FINISHED_EARLY, "Expected ride status to be FINISHED EARLY but got " + ride.getStatus());
    verify(rideRepository).saveAndFlush(ride);
    verify(driverRepository).save(driver);
  }

  @Test
  public void test_finish_ride_when_driver_has_next_ride() {
    Ride nextRide = createActiveRide(new Passenger());
    nextRide.setStatus(RideStatus.PENDING);

    Ride currentRide = createActiveRide(new Passenger());
    Driver driver = createDriverWithActiveRide(currentRide);

    nextRide.setDriver(driver);
    nextRide.setScheduledAt(LocalDateTime.now().plusMinutes(10));

    when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
    when(rideRepository.saveAndFlush(currentRide)).thenReturn(currentRide);
    when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.PENDING)).thenReturn(List.of(nextRide));
    when(routeCalculationService.calculateRoute(any(), any(), any())).thenReturn(createRouteInfo());
    when(driverRepository.save(driver)).thenReturn(driver);

    driverService.finishRide(1L, new Location(45.05, 19.05, "Stop 1"));

    assertEquals(currentRide.getStatus(), RideStatus.FINISHED_EARLY, "Expected ride status to be FINISHED EARLY but got " + currentRide.getStatus());
    assertEquals(driver.getCurrentRide(), nextRide, "Expected driver currentRide to be the next ride but got " + driver.getCurrentRide());
    assertFalse(driver.isAvailable(), "Expected driver to not be available because there is a next ride");

    verify(rideRepository).saveAndFlush(currentRide);
    verify(driverRepository).save(driver);
  }

  @Test
  public void test_finish_ride_when_driver_has_no_next_ride() {
    Ride currentRide = createActiveRide(new Passenger());
    Driver driver = createDriverWithActiveRide(currentRide);

    when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
    when(rideRepository.saveAndFlush(currentRide)).thenReturn(currentRide);
    when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.PENDING)).thenReturn(List.of());
    when(driverRepository.save(driver)).thenReturn(driver);

    driverService.finishRide(1L, new Location(45.1, 19.1, "End"));

    assertEquals(currentRide.getStatus(), RideStatus.COMPLETED, "Expected ride status to be COMPLETED but got " + currentRide.getStatus());
    assertNull(driver.getCurrentRide(), "Expected driver currentRide to be null but got " + driver.getCurrentRide());
    assertTrue(driver.isAvailable(), "Expected driver to be available because there are no next rides");

    verify(rideRepository).saveAndFlush(currentRide);
    verify(driverRepository).save(driver);
  }


}
