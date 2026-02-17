package com.st3.uber.services;

import com.st3.uber.domain.*;
import com.st3.uber.dto.route.RouteInfo;
import com.st3.uber.enums.NotificationType;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.RideRepository;
import com.st3.uber.service.*;
import com.st3.uber.util.GetAddressFromLatLng;
import org.mockito.MockedStatic;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import org.testng.annotations.*;

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

  private static final long DRIVER_ID = 1L;
  private static final long RIDE_ID = 1L;
  private static final long PASSENGER_ID = 1L;
  private static final String PASSENGER_EMAIL = "example@gmail.com";
  private static final String PASSENGER_NAME = "name";
  private static final Location endLocation = new Location(45.1, 19.1, "End");
  private MockedStatic<GetAddressFromLatLng> addressMock;

  @BeforeClass
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  @BeforeMethod
  public void resetMocks() {
    reset(driverRepository, rideRepository, routeCalculationService, notificationService, rideService, mailService);
    addressMock = mockStatic(GetAddressFromLatLng.class);
    addressMock.when(() -> GetAddressFromLatLng.addressFromLatLng(anyDouble(), anyDouble()))
        .thenReturn("Mocked address");
  }

  @AfterMethod
  public void tearDownMethod() {
    if (addressMock != null) {
      addressMock.close();
    }
  }

  @Test(expectedExceptions = ResponseStatusException.class,
      expectedExceptionsMessageRegExp = "404 NOT_FOUND \"Driver not found\"")
  public void test_finish_ride_when_driver_not_found(){
    when(driverRepository.findById(anyLong())).thenReturn(Optional.empty());

    driverService.finishRide(RIDE_ID,new Location());

    verifyNoInteractions(rideRepository);
  }

  @Test(expectedExceptions = ResponseStatusException.class,
      expectedExceptionsMessageRegExp = "400 BAD_REQUEST \"Driver has no active ride\"")
  public void test_finish_ride_when_ride_not_found(){
    Driver driver = createDriverWithActiveRide(null);
    when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));

    driverService.finishRide(DRIVER_ID,new Location());

    addressMock.verifyNoInteractions();
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Ride not found")
  public void test_finish_ride_when_ride_not_found_in_base(){
    Ride ride = createActiveRide(existingPassenger());
    Driver driver = createDriverWithActiveRide(ride);

    when(driverRepository.findById(RIDE_ID)).thenReturn(Optional.of(driver));
    when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.empty());

    driverService.finishRide(DRIVER_ID,new Location());

    addressMock.verifyNoInteractions();
  }

  @Test(expectedExceptions = ResponseStatusException.class,
      expectedExceptionsMessageRegExp = "400 BAD_REQUEST \"Can only finish rides that are in progress\"",
      dataProvider = "rideStatuses")
  public void test_finish_ride_when_ride_not_in_progress(RideStatus status){
    Ride ride = createActiveRide(existingPassenger());
    ride.setStatus(status);
    Driver driver = createDriverWithActiveRide(ride);

    when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
    when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(ride));

    driverService.finishRide(DRIVER_ID,new Location());

    addressMock.verifyNoInteractions();
  }

  @DataProvider(name = "rideStatuses")
  public Object[][] rideStatuses() {
    return new Object[][]{
        { RideStatus.COMPLETED },
        { RideStatus.ACCEPTED },
        { RideStatus.FINISHED_EARLY },
        { RideStatus.CANCELLED_BY_DRIVER },
        { RideStatus.CANCELLED_BY_PASSENGER },
        { RideStatus.REJECTED },
        { RideStatus.PENDING }
    };
  }

  private Driver createDriverWithActiveRide(Ride ride) {
    Driver driver = new Driver();

    driver.setId(DRIVER_ID);
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
    ride.setPassengers(new ArrayList<>(List.of(passenger, passenger(2L, "new name", "a2@gmail.com"))));
    ride.setStartLocation(new Location(45.0, 19.0, "Start"));
    ride.setEndLocation(endLocation);
    ride.setInvites(new ArrayList<>());
    ride.setRideStops(new ArrayList<>());
    ride.setActualRideStops(new ArrayList<>());

    return ride;
  }

  @Test(expectedExceptions = IllegalStateException.class,
      expectedExceptionsMessageRegExp = "It should get valid end location")
  public void test_finish_ride_with_no_end_location(){
    Ride ride = createActiveRide(existingPassenger());
    Driver driver = createDriverWithActiveRide(ride);

    when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
    when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(ride));

    driverService.finishRide(DRIVER_ID, null);

    addressMock.verifyNoInteractions();
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
    Location endLocationCompleted = new Location(80.05, 100.05, "End");

    Ride ride = createActiveRide(existingPassenger());
    ride.setEndLocation(endLocationCompleted);
    Driver driver = createDriverWithActiveRide(ride);

    when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
    when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(ride));
    when(rideRepository.saveAndFlush(ride)).thenReturn(ride);
    when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED)).thenReturn(List.of());
    when(driverRepository.save(driver)).thenReturn(driver);

    Ride result = driverService.finishRide(DRIVER_ID, endLocationCompleted);

    assertEquals(result.getStatus(), RideStatus.COMPLETED, "Expected ride status to be COMPLETED but got " + ride.getStatus());
    verifyNoInteractions(routeCalculationService);
    verify(rideRepository).saveAndFlush(ride);
    verify(driverRepository).save(driver);
    assertTrue(driver.isAvailable(), "Expected driver to be available after finishing ride");
    assertTrue(driver.isFree(), "Expected driver to be free after finishing ride");
    assertNotNull(result.getFinishedAt());
    assertNotNull(result.getActualEndLocation());
    assertEquals(result.getActualEndLocation().getAddress(), "Mocked address");

  }

  @Test
  public void test_finish_ride_when_finished_early() {
    Location stopLocation = new Location(45.05, 19.05, "Stop 1");

    Ride ride = createActiveRide(existingPassenger());
    ride.setEndLocation(endLocation);
    Driver driver = createDriverWithActiveRide(ride);

    when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
    when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(ride));
    when(rideRepository.saveAndFlush(ride)).thenReturn(ride);
    when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED)).thenReturn(List.of());
    when(routeCalculationService.calculateRoute(eq(ride.getStartLocation()), eq(stopLocation), eq(ride.getActualRideStops()))).thenReturn(createRouteInfo());
    when(driverRepository.save(driver)).thenReturn(driver);

    driverService.finishRide(DRIVER_ID, stopLocation);

    assertEquals(ride.getStatus(), RideStatus.FINISHED_EARLY, "Expected ride status to be FINISHED EARLY but got " + ride.getStatus());
    verify(rideRepository).saveAndFlush(ride);
    verify(driverRepository).save(driver);
    assertTrue(driver.isAvailable(), "Expected driver to be available after finishing ride");
    assertTrue(driver.isFree(), "Expected driver to be free after finishing ride");
  }

  @Test
  public void test_finish_ride_when_driver_has_next_ride() {
    Ride nextRide = createActiveRide(existingPassenger());
    nextRide.setStatus(RideStatus.ACCEPTED);

    Ride currentRide = createActiveRide(passenger(2L, "new name", "a2@gmail.com"));
    Driver driver = createDriverWithActiveRide(currentRide);

    nextRide.setDriver(driver);
    nextRide.setScheduledAt(LocalDateTime.now().plusMinutes(10));

    when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
    when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(currentRide));
    when(rideRepository.saveAndFlush(currentRide)).thenReturn(currentRide);
    when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED)).thenReturn(List.of(nextRide));
    when(routeCalculationService.calculateRoute(any(), any(), any())).thenReturn(createRouteInfo());
    when(driverRepository.save(driver)).thenReturn(driver);

    driverService.finishRide(DRIVER_ID, new Location(45.05, 19.05, "Stop 1"));

    assertEquals(currentRide.getStatus(), RideStatus.FINISHED_EARLY, "Expected ride status to be FINISHED EARLY but got " + currentRide.getStatus());
    assertEquals(driver.getCurrentRide(), nextRide, "Expected driver currentRide to be the next ride but got " + driver.getCurrentRide());
    assertFalse(driver.isAvailable(), "Expected driver to not be available because there is a next ride");

    verify(rideRepository).saveAndFlush(currentRide);
    verify(driverRepository).save(driver);
    assertFalse(driver.isAvailable(), "Expected driver to not be available because there is a next ride");
  }

  @Test
  public void test_finish_ride_when_driver_has_no_next_ride() {
    Ride currentRide = createActiveRide(existingPassenger());
    Driver driver = createDriverWithActiveRide(currentRide);

    when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
    when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(currentRide));
    when(rideRepository.saveAndFlush(currentRide)).thenReturn(currentRide);
    when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED)).thenReturn(List.of());
    when(driverRepository.save(driver)).thenReturn(driver);

    driverService.finishRide(DRIVER_ID, endLocation);

    assertEquals(currentRide.getStatus(), RideStatus.COMPLETED, "Expected ride status to be COMPLETED but got " + currentRide.getStatus());
    assertNull(driver.getCurrentRide(), "Expected driver currentRide to be null but got " + driver.getCurrentRide());
    assertTrue(driver.isAvailable(), "Expected driver to be available because there are no next rides");

    verify(rideRepository).saveAndFlush(currentRide);
    verify(driverRepository).save(driver);
    assertTrue(driver.isAvailable(), "Expected driver to be available because there are no next rides");
  }

  private Passenger passenger(Long id, String name, String email) {
    Passenger p = new Passenger();
    p.setId(id);
    p.setName(name);
    p.setEmail(email);
    return p;
  }

  private RideInvite invite(String email) {
    RideInvite i = new RideInvite();
    i.setEmail(email);
    return i;
  }

  private Ride baseRide() {
    Ride r = createActiveRide(passenger(1L, "name", "a@gmail.com"));
    r.setBasePrice(100.0);
    r.setCalculatedPrice(500.0);
    r.setEstimatedTimeMinutes(15);
    r.setDistance(10.0);
    return r;
  }

  @Test(dataProvider = "rideStatusData")
  public void test_finish_ride_with_noInvites_noNextRide(Location plannedEnd, Location actualEnd, RideStatus expectedStatus) {
    Ride ride = baseRide();

    ride.setEndLocation(plannedEnd);
    Driver driver = createDriverWithActiveRide(ride);

    when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
    when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(ride));
    when(rideRepository.saveAndFlush(ride)).thenReturn(ride);
    when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED)).thenReturn(List.of());
    when(routeCalculationService.calculateRoute(any(), any(), any())).thenReturn(createRouteInfo());
    when(driverRepository.save(driver)).thenReturn(driver);

    driverService.finishRide(DRIVER_ID, actualEnd);

    assertEquals(ride.getStatus(), expectedStatus, "Expected ride status to be " + expectedStatus + " but got " + ride.getStatus());
    verify(rideRepository).saveAndFlush(ride);

    int n = ride.getPassengers().size();

    verify(notificationService, times(n)).createNotification(anyLong(), anyString(), eq(NotificationType.FINISHED_RIDE), eq(ride.getId()));
    verify(mailService, times(n)).sendText(anyString(), anyString(), anyString());

    verify(notificationService).createNotification(eq(driver.getId()), anyString(), eq(NotificationType.PROFILE_CHANGE), isNull());
    assertTrue(driver.isAvailable());
    verify(driverRepository).save(driver);
  }

  @Test(dataProvider = "rideStatusData")
  public void test_finish_ride_with_noNextRide(Location plannedEnd, Location actualEnd, RideStatus expectedStatus) {
    Ride ride = baseRide();
    RideInvite invite = invite("invite@gmail.com");
    ride.setInvites(List.of(invite));

    ride.setEndLocation(plannedEnd);
    Driver driver = createDriverWithActiveRide(ride);

    when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
    when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(ride));
    when(rideRepository.saveAndFlush(ride)).thenReturn(ride);
    when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED)).thenReturn(List.of());
    when(routeCalculationService.calculateRoute(any(), any(), any())).thenReturn(createRouteInfo());
    when(driverRepository.save(driver)).thenReturn(driver);

    driverService.finishRide(DRIVER_ID, actualEnd);

    assertEquals(ride.getStatus(), expectedStatus, "Expected ride status to be " + expectedStatus + " but got " + ride.getStatus());
    verify(rideRepository).saveAndFlush(ride);

    int n = ride.getPassengers().size();
    int inviteCount = ride.getInvites().size();

    verify(notificationService, times(n)).createNotification(anyLong(), anyString(), eq(NotificationType.FINISHED_RIDE), eq(ride.getId()));
    verify(mailService, times(inviteCount + n)).sendText(anyString(), anyString(), anyString());
    verifyNoMoreInteractions(mailService);

    verify(notificationService).createNotification(eq(driver.getId()), anyString(), eq(NotificationType.PROFILE_CHANGE), isNull());
    assertTrue(driver.isAvailable());
    verify(driverRepository).save(driver);
  }

  @Test(dataProvider = "rideStatusData")
  public void test_finish_ride(Location plannedEnd, Location actualEnd, RideStatus expectedStatus) {
    Ride ride = baseRide();
    RideInvite invite = invite("invite@gmail.com");
    ride.setInvites(List.of(invite));

    ride.setEndLocation(plannedEnd);
    Driver driver = createDriverWithActiveRide(ride);

    when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
    when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(ride));
    when(rideRepository.saveAndFlush(ride)).thenReturn(ride);
    when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED)).thenReturn(List.of());
    when(routeCalculationService.calculateRoute(any(), any(), any())).thenReturn(createRouteInfo());
    when(driverRepository.save(driver)).thenReturn(driver);

    driverService.finishRide(DRIVER_ID, actualEnd);

    assertEquals(ride.getStatus(), expectedStatus, "Expected ride status to be " + expectedStatus + " but got " + ride.getStatus());
    verify(rideRepository).saveAndFlush(ride);

    int n = ride.getPassengers().size();
    int inviteCount = ride.getInvites().size();

    verify(notificationService, times(n)).createNotification(anyLong(), anyString(), eq(NotificationType.FINISHED_RIDE), eq(ride.getId()));
    verify(mailService, times(inviteCount + n)).sendText(anyString(), anyString(), anyString());
    verifyNoMoreInteractions(mailService);

    verify(notificationService).createNotification(eq(driver.getId()), anyString(), eq(NotificationType.PROFILE_CHANGE), isNull());
    assertTrue(driver.isAvailable());
    verify(driverRepository).save(driver);
  }
  @DataProvider(name = "rideStatusData")
  public Object[][] rideStatusData() {

    Location plannedEnd = new Location(44.817000, 20.457000, "Planned End");
    Location actualEndClose = new Location(44.817100, 20.457100, "Actual End (close)");
    Location actualEndFar = new Location(44.826000, 20.467000, "Actual End (far)");

    return new Object[][]{
        { plannedEnd, actualEndClose, RideStatus.COMPLETED },
        { plannedEnd, actualEndFar,   RideStatus.FINISHED_EARLY }
    };
  }

  private Passenger existingPassenger(){
    return passenger(PASSENGER_ID, PASSENGER_NAME, PASSENGER_EMAIL);
  }
}