package com.st3.uber.s2.service;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Location;
import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.dto.route.RouteInfo;
import com.st3.uber.enums.NotificationType;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.VehicleType;
import com.st3.uber.repository.DriverRepository;
import com.st3.uber.repository.RideRepository;
import com.st3.uber.service.DriverService;
import com.st3.uber.service.MailService;
import com.st3.uber.service.NotificationService;
import com.st3.uber.service.RouteCalculationService;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for DriverService.finishRide() — Functionality 2.7 (Završetak vožnje).
 *
 * Private methods (finishedEarly, isWithinRadius, sendNotifications)
 *
 * GetAddressFromLatLng.addressFromLatLng() is a static utility called inside
 * finishRide() — also tested indirectly
 */
public class DriverServiceTest {

    private DriverRepository driverRepository;
    private RideRepository rideRepository;
    private NotificationService notificationService;
    private RouteCalculationService routeCalculationService;
    private MailService mailService;

    private DriverService driverService;

    private static final Long DRIVER_ID = 10L;
    private static final Long RIDE_ID   = 100L;

    private static final Location NORMAL_END =
            new Location(45.2702, 19.8401, "Trg slobode");

    private static final Location EARLY_END =
            new Location(45.3000, 19.9000, "Far away place");

    private static final Location PLANNED_END =
            new Location(45.2702, 19.8401, "Trg slobode");


    @BeforeSuite
    public void setupSuite() {
        driverRepository        = Mockito.mock(DriverRepository.class);
        rideRepository          = Mockito.mock(RideRepository.class);
        notificationService     = Mockito.mock(NotificationService.class);
        routeCalculationService = Mockito.mock(RouteCalculationService.class);
        mailService             = Mockito.mock(MailService.class);

        driverService = new DriverService(
                driverRepository,
                notificationService,
                rideRepository,
                routeCalculationService,
                mailService
        );
    }

    @BeforeMethod
    public void resetMocks() {
        Mockito.reset(driverRepository, rideRepository,
                notificationService, routeCalculationService, mailService);
    }


    private Passenger buildPassenger(Long id, String email) {
        Passenger p = new Passenger();
        p.setId(id);
        p.setEmail(email);
        p.setName("Marko");
        p.setSurname("Markovic");
        return p;
    }

    private Ride buildInProgressRide() {
        Passenger passenger = buildPassenger(1L, "passenger@test.com");

        Ride ride = new Ride();
        ride.setId(RIDE_ID);
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(LocalDateTime.now().minusMinutes(20));
        ride.setCreatedAt(LocalDateTime.now().minusMinutes(25));
        ride.setEstimatedTimeMinutes(15);
        ride.setVehicleType(VehicleType.STANDARD);
        ride.setDistance(5.0);
        ride.setBasePrice(100.0);
        ride.setCalculatedPrice(700.0);
        ride.setBabyTransport(false);
        ride.setPetTransport(false);
        ride.setStartLocation(new Location(45.2671, 19.8335, "Bulevar oslobodenja"));
        ride.setEndLocation(PLANNED_END);
        ride.setCreator(passenger);
        ride.getPassengers().add(passenger);
        return ride;
    }

    private Driver buildDriverWithRide(Ride ride) {
        Driver driver = new Driver();
        driver.setId(DRIVER_ID);
        driver.setName("Petar");
        driver.setSurname("Petrovic");
        driver.setActive(true);
        driver.setAvailable(false);
        driver.setFree(false);
        driver.setCurrentRide(ride);
        return driver;
    }

    /** Stubs the standard happy-path repository calls. No next rides by default. */
    private void stubHappyPath(Driver driver, Ride ride) {
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(ride));
        when(rideRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(driverRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED))
                .thenReturn(List.of());
    }

    // ════════════════════════════════════════════════════════════════════════
    // I. VALIDATION / EXCEPTION CASES
    // ════════════════════════════════════════════════════════════════════════

    @Test(expectedExceptions = ResponseStatusException.class,
            description = "I.1 – Driver not found in DB")
    public void finishRide_driverNotFound_shouldThrow() {
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.empty());

        try {
            driverService.finishRide(DRIVER_ID, NORMAL_END);
        } finally {
            // findById was attempted exactly once; nothing else should have been touched
            verify(driverRepository, times(1)).findById(DRIVER_ID);
            verify(rideRepository, never()).findByIdWithLock(anyLong());
            verify(rideRepository, never()).saveAndFlush(any());
            verify(notificationService, never()).createNotification(anyLong(), anyString(), any(), any());
            verify(mailService, never()).sendText(anyString(), anyString(), anyString());
        }
    }

    @Test(expectedExceptions = ResponseStatusException.class,
            description = "I.2 – Driver exists but has no current ride")
    public void finishRide_driverHasNoCurrentRide_shouldThrow() {
        Driver driver = buildDriverWithRide(null); // currentRide = null
        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));

        try {
            driverService.finishRide(DRIVER_ID, NORMAL_END);
        } finally {
            // Driver was looked up; the ride lock query must never have been reached
            verify(driverRepository, times(1)).findById(DRIVER_ID);
            verify(rideRepository, never()).findByIdWithLock(anyLong());
            verify(rideRepository, never()).saveAndFlush(any());
            verify(notificationService, never()).createNotification(anyLong(), anyString(), any(), any());
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            description = "I.3 – findByIdWithLock returns empty (data inconsistency)")
    public void finishRide_rideNotFoundInRepository_shouldThrow() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);

        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.empty());

        try {
            driverService.finishRide(DRIVER_ID, NORMAL_END);
        } finally {
            // Lock query was called once, but save was never reached
            verify(driverRepository, times(1)).findById(DRIVER_ID);
            verify(rideRepository, times(1)).findByIdWithLock(RIDE_ID);
            verify(rideRepository, never()).saveAndFlush(any());
            verify(notificationService, never()).createNotification(anyLong(), anyString(), any(), any());
        }
    }

    @Test(expectedExceptions = ResponseStatusException.class,
            description = "I.4a – Ride status is PENDING (not IN_PROGRESS)")
    public void finishRide_rideStatusPending_shouldThrow() {
        Ride ride = buildInProgressRide();
        ride.setStatus(RideStatus.PENDING);
        Driver driver = buildDriverWithRide(ride);

        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(ride));

        try {
            driverService.finishRide(DRIVER_ID, NORMAL_END);
        } finally {
            verify(rideRepository, times(1)).findByIdWithLock(RIDE_ID);
            // Status check fails before any persistence happens
            verify(rideRepository, never()).saveAndFlush(any());
            verify(notificationService, never()).createNotification(anyLong(), anyString(), any(), any());
            verify(mailService, never()).sendText(anyString(), anyString(), anyString());
        }
    }

    @Test(expectedExceptions = ResponseStatusException.class,
            description = "I.4b – Ride status is ACCEPTED (driver never started it)")
    public void finishRide_rideStatusAccepted_shouldThrow() {
        Ride ride = buildInProgressRide();
        ride.setStatus(RideStatus.ACCEPTED);
        Driver driver = buildDriverWithRide(ride);

        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(ride));

        try {
            driverService.finishRide(DRIVER_ID, NORMAL_END);
        } finally {
            verify(rideRepository, times(1)).findByIdWithLock(RIDE_ID);
            verify(rideRepository, never()).saveAndFlush(any());
            verify(notificationService, never()).createNotification(anyLong(), anyString(), any(), any());
        }
    }

    @Test(expectedExceptions = ResponseStatusException.class,
            description = "I.4c – Ride status is COMPLETED (already finished)")
    public void finishRide_rideAlreadyCompleted_shouldThrow() {
        Ride ride = buildInProgressRide();
        ride.setStatus(RideStatus.COMPLETED);
        Driver driver = buildDriverWithRide(ride);

        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(ride));

        try {
            driverService.finishRide(DRIVER_ID, NORMAL_END);
        } finally {
            verify(rideRepository, never()).saveAndFlush(any());
            verify(notificationService, never()).createNotification(anyLong(), anyString(), any(), any());
        }
    }

    @Test(expectedExceptions = IllegalStateException.class,
            description = "I.5 – actualEndLocation is null")
    public void finishRide_nullActualEndLocation_shouldThrow() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);

        when(driverRepository.findById(DRIVER_ID)).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdWithLock(RIDE_ID)).thenReturn(Optional.of(ride));

        try {
            driverService.finishRide(DRIVER_ID, null);
        } finally {
            // Null-check fires after the ride is loaded but before any save
            verify(rideRepository, times(1)).findByIdWithLock(RIDE_ID);
            verify(rideRepository, never()).saveAndFlush(any());
            verify(routeCalculationService, never()).calculateRoute(any(), any(), any());
            verify(notificationService, never()).createNotification(anyLong(), anyString(), any(), any());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // II. NORMAL FLOW (COMPLETED)
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "II.1 – Normal finish at planned endpoint → status COMPLETED")
    public void finishRide_normalEnd_statusShouldBeCompleted() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        Ride result = driverService.finishRide(DRIVER_ID, NORMAL_END);

        assertEquals(result.getStatus(), RideStatus.COMPLETED);
        // saveAndFlush must have been called exactly once with the updated ride
        verify(rideRepository, times(1)).saveAndFlush(any(Ride.class));
    }

    @Test(description = "II.1 – finishedAt must be set on a normal finish")
    public void finishRide_normalEnd_shouldSetFinishedAt() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        LocalDateTime before = LocalDateTime.now();
        Ride result = driverService.finishRide(DRIVER_ID, NORMAL_END);
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(result.getFinishedAt());
        assertFalse(result.getFinishedAt().isBefore(before));
        assertFalse(result.getFinishedAt().isAfter(after));
        verify(rideRepository, times(1)).saveAndFlush(any(Ride.class));
    }

    @Test(description = "II.1 – actualEndLocation must be stored on the ride")
    public void finishRide_normalEnd_shouldSetActualEndLocation() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        Ride result = driverService.finishRide(DRIVER_ID, NORMAL_END);

        assertNotNull(result.getActualEndLocation());
        // Verify the ride was flushed after setting the location
        verify(rideRepository, times(1)).saveAndFlush(any(Ride.class));
    }

    @Test(description = "II.1 – GetAddressFromLatLng called: " +
            "when Nominatim returns 429, address defaults to 'Unknown Location' " +
            "but finishRide still completes successfully")
    public void finishRide_addressLookupFails_shouldStillComplete() {
        // addressFromLatLng is a static call inside finishRide().
        // In unit tests it hits the real Nominatim API (or fails with 429).
        // The catch block in GetAddressFromLatLng returns "Unknown Location" so
        // the ride must still be saved with COMPLETED status regardless.
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        Location loc = new Location(45.2702, 19.8401, null);

        Ride result = driverService.finishRide(DRIVER_ID, loc);

        assertEquals(result.getStatus(), RideStatus.COMPLETED);
        verify(rideRepository, times(1)).saveAndFlush(any(Ride.class));
        // The address may be null, "Unknown Location", or a real street name —
        // all are acceptable outcomes; what matters is no exception was thrown
    }

    @Test(description = "II.1 – driver.currentRide cleared and driver.free=true after finish")
    public void finishRide_normalEnd_shouldFreeDriver() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        // Verify driver was saved with free=true and no current ride
        verify(driverRepository, times(1)).save(argThat(d -> {
            Driver saved = (Driver) d;
            return saved.isFree() && saved.getCurrentRide() == null;
        }));
    }

    @Test(description = "II.1 – rideRepository.saveAndFlush called exactly once")
    public void finishRide_normalEnd_shouldCallSaveAndFlushOnce() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        verify(rideRepository, times(1)).saveAndFlush(any(Ride.class));
        // Regular save() on the ride must NOT be called — only saveAndFlush
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test(description = "II.1 – routeCalculationService NOT called on normal finish")
    public void finishRide_normalEnd_shouldNotCallRouteCalculation() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        // isWithinRadius = true → finishedEarly() returns false →
        // calculateRoute must never be called
        verify(routeCalculationService, never()).calculateRoute(any(), any(), any());
    }

    @Test(description = "II.1 – driverRepository.save called exactly once")
    public void finishRide_normalEnd_shouldCallDriverSaveOnce() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    // ════════════════════════════════════════════════════════════════════════
    // III. EARLY FINISH (FINISHED_EARLY)
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "III.1 – Early finish → status FINISHED_EARLY")
    public void finishRide_earlyEnd_statusShouldBeFinishedEarly() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        when(routeCalculationService.calculateRoute(any(), any(), anyList()))
                .thenReturn(new RouteInfo(2.0, 8));

        Ride result = driverService.finishRide(DRIVER_ID, EARLY_END);

        assertEquals(result.getStatus(), RideStatus.FINISHED_EARLY);
    }

    @Test(description = "III.1 – routeCalculationService.calculateRoute called exactly once for early finish")
    public void finishRide_earlyEnd_shouldCallRouteCalculationOnce() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        when(routeCalculationService.calculateRoute(any(), any(), anyList()))
                .thenReturn(new RouteInfo(2.0, 8));

        driverService.finishRide(DRIVER_ID, EARLY_END);

        // finishedEarly() is private — verified indirectly: calculateRoute must
        // have been called exactly once with the start and actual-end locations
        verify(routeCalculationService, times(1))
                .calculateRoute(any(Location.class), any(Location.class), anyList());
    }

    @Test(description = "III.1 – distance recalculated to actual route length")
    public void finishRide_earlyEnd_shouldRecalculateDistance() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        when(routeCalculationService.calculateRoute(any(), any(), anyList()))
                .thenReturn(new RouteInfo(2.0, 8));

        Ride result = driverService.finishRide(DRIVER_ID, EARLY_END);

        assertEquals(result.getDistance(), 2.0, 0.001);
    }

    @Test(description = "III.1 – price recalculated: basePrice + distance * 120")
    public void finishRide_earlyEnd_shouldRecalculatePrice() {
        Ride ride = buildInProgressRide(); // basePrice = 100
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        when(routeCalculationService.calculateRoute(any(), any(), anyList()))
                .thenReturn(new RouteInfo(3.0, 10));

        Ride result = driverService.finishRide(DRIVER_ID, EARLY_END);

        // 100 + 3.0 * 120 = 460
        assertEquals(result.getCalculatedPrice(), 460.0, 0.001);
        verify(routeCalculationService, times(1)).calculateRoute(any(), any(), anyList());
    }

    @Test(description = "III.1 – ride saved and driver freed even on early finish")
    public void finishRide_earlyEnd_shouldSaveRideAndFreeDriver() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        when(routeCalculationService.calculateRoute(any(), any(), anyList()))
                .thenReturn(new RouteInfo(2.0, 8));

        driverService.finishRide(DRIVER_ID, EARLY_END);

        verify(rideRepository, times(1)).saveAndFlush(any(Ride.class));
        verify(driverRepository, times(1)).save(argThat(d ->
                ((Driver) d).isFree() && ((Driver) d).getCurrentRide() == null));
    }

    // ════════════════════════════════════════════════════════════════════════
    // IV. RIDE STOPS LOGIC
    // (private logic inside finishRide — tested indirectly via result state)
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "IV – no planned stops → actualRideStops stays empty")
    public void finishRide_noPlannedStops_actualStopsShouldRemainEmpty() {
        Ride ride = buildInProgressRide(); // rideStops empty by default

        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        Ride result = driverService.finishRide(DRIVER_ID, NORMAL_END);

        assertTrue(result.getActualRideStops().isEmpty());
        verify(rideRepository, times(1)).saveAndFlush(any(Ride.class));
    }

    // ════════════════════════════════════════════════════════════════════════
    // V. NEXT RIDES
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "V.1 – driver has next scheduled ride → currentRide updated, available=false")
    public void finishRide_withNextRide_shouldUpdateDriverCurrentRide() {
        Ride currentRide = buildInProgressRide();
        Driver driver = buildDriverWithRide(currentRide);
        stubHappyPath(driver, currentRide);

        Ride nextRide = new Ride();
        nextRide.setId(200L);
        nextRide.setStatus(RideStatus.ACCEPTED);
        nextRide.setScheduledAt(LocalDateTime.now().plusMinutes(30));
        nextRide.setDriver(driver);

        when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED))
                .thenReturn(List.of(nextRide));

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        // sendNotifications() is private; verified indirectly:
        // driver must be saved with the next ride assigned and available=false
        verify(driverRepository, times(1)).save(argThat(d -> {
            Driver saved = (Driver) d;
            return saved.getCurrentRide() != null
                    && saved.getCurrentRide().getId().equals(200L)
                    && !saved.isAvailable();
        }));
    }

    @Test(description = "V.1 – RIDE_REMINDER notification sent to driver when next ride exists")
    public void finishRide_withNextRide_shouldSendRideReminderNotificationToDriver() {
        Ride currentRide = buildInProgressRide();
        Driver driver = buildDriverWithRide(currentRide);
        stubHappyPath(driver, currentRide);

        Ride nextRide = new Ride();
        nextRide.setId(200L);
        nextRide.setStatus(RideStatus.ACCEPTED);
        nextRide.setScheduledAt(LocalDateTime.now().plusMinutes(30));
        nextRide.setDriver(driver);

        when(rideRepository.findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED))
                .thenReturn(List.of(nextRide));

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        // At least one RIDE_REMINDER notification sent (to driver for next ride)
        verify(notificationService, atLeastOnce())
                .createNotification(anyLong(), anyString(),
                        eq(NotificationType.RIDE_REMINDER), any());
    }

    @Test(description = "V.2 – no next ride → driver.available=true")
    public void finishRide_noNextRide_driverShouldBecomeAvailable() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride); // returns empty list for next rides

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        verify(driverRepository, times(1)).save(argThat(d ->
                ((Driver) d).isAvailable()
        ));
    }


    // ════════════════════════════════════════════════════════════════════════
    // VI. NOTIFICATIONS AND EMAIL
    // (sendNotifications() is private — tested indirectly through finishRide)
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "VI.1 – FINISHED_RIDE notification sent once for single passenger")
    public void finishRide_singlePassenger_shouldSendOneFinishedRideNotification() {
        Ride ride = buildInProgressRide(); // 1 passenger
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        verify(notificationService, times(1))
                .createNotification(
                        eq(1L),                          // passenger id
                        anyString(),
                        eq(NotificationType.FINISHED_RIDE),
                        eq(RIDE_ID)
                );
    }

    @Test(description = "VI.1 – FINISHED_RIDE notification sent to every passenger (N=2)")
    public void finishRide_twoPassengers_shouldSendTwoFinishedRideNotifications() {
        Ride ride = buildInProgressRide();
        Passenger second = buildPassenger(2L, "second@test.com");
        ride.getPassengers().add(second); // now 2 passengers

        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        // One FINISHED_RIDE notification per passenger
        verify(notificationService, times(1))
                .createNotification(eq(1L), anyString(), eq(NotificationType.FINISHED_RIDE), eq(RIDE_ID));
        verify(notificationService, times(1))
                .createNotification(eq(2L), anyString(), eq(NotificationType.FINISHED_RIDE), eq(RIDE_ID));
    }

    @Test(description = "VI.2 – mailService.sendText called once per passenger")
    public void finishRide_singlePassenger_shouldSendOneEmail() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        verify(mailService, times(1))
                .sendText(eq("passenger@test.com"), anyString(), anyString());
    }

    @Test(description = "VI.2 – mailService.sendText called for each passenger (N=2)")
    public void finishRide_twoPassengers_shouldSendTwoEmails() {
        Ride ride = buildInProgressRide();
        Passenger second = buildPassenger(2L, "second@test.com");
        ride.getPassengers().add(second);

        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        verify(mailService, times(1)).sendText(eq("passenger@test.com"), anyString(), anyString());
        verify(mailService, times(1)).sendText(eq("second@test.com"), anyString(), anyString());
    }

    @Test(description = "VI.3 – mailService throws exception → finishRide still succeeds")
    public void finishRide_mailServiceThrows_shouldNotPropagateException() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        // Simulate a mail server error
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(mailService).sendText(anyString(), anyString(), anyString());

        // finishRide must complete without throwing
        Ride result = driverService.finishRide(DRIVER_ID, NORMAL_END);

        assertNotNull(result);
        assertEquals(result.getStatus(), RideStatus.COMPLETED);

        // Mail was attempted — the exception was swallowed internally
        verify(mailService, times(1)).sendText(anyString(), anyString(), anyString());
        // Ride and driver were still saved successfully
        verify(rideRepository, times(1)).saveAndFlush(any(Ride.class));
        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    @Test(description = "VI.3 – mail exception does not prevent notifications from being sent")
    public void finishRide_mailServiceThrows_notificationsShouldStillBeSent() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        doThrow(new RuntimeException("SMTP down"))
                .when(mailService).sendText(anyString(), anyString(), anyString());

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        // Notifications are sent before email; mail failure must not block them
        verify(notificationService, atLeastOnce())
                .createNotification(anyLong(), anyString(), any(), any());
    }

    // ════════════════════════════════════════════════════════════════════════
    // BOUNDARY CASES
    // ════════════════════════════════════════════════════════════════════════

    @Test(description = "Same coords as planned end → isWithinRadius=true → COMPLETED, no route call")
    public void finishRide_exactlyAtPlannedEnd_shouldComplete_andNotCallRoute() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        Ride result = driverService.finishRide(DRIVER_ID, PLANNED_END);

        assertEquals(result.getStatus(), RideStatus.COMPLETED);
        verify(routeCalculationService, never()).calculateRoute(any(), any(), any());
        verify(rideRepository, times(1)).saveAndFlush(any(Ride.class));
    }

    @Test(description = "Invalid lat/lng sent to GetAddressFromLatLng → 'Unknown Location' returned, ride still saved")
    public void finishRide_invalidCoordinates_addressDefaultsToUnknown() {
        Ride ride = buildInProgressRide();
        // Set planned end to same coords so it counts as normal finish
        ride.setEndLocation(new Location(0.0, 0.0, "Equator"));
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        // Coordinates that are valid range-wise but unusual
        Location loc = new Location(0.0, 0.0, null);

        Ride result = driverService.finishRide(DRIVER_ID, loc);
        assertEquals(result.getActualEndLocation().getAddress(),"Unknown Location");
        // Ride must still be saved regardless of what addressFromLatLng returns
        verify(rideRepository, times(1)).saveAndFlush(any(Ride.class));
        assertNotNull(result);
    }

    @Test(description = "findByStatusAndScheduledAtIsNotNull called exactly once to check next rides")
    public void finishRide_shouldQueryNextRidesExactlyOnce() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        verify(rideRepository, times(1))
                .findByStatusAndScheduledAtIsNotNull(RideStatus.ACCEPTED);
    }

    @Test(description = "driverRepository.findById called exactly once at the start")
    public void finishRide_shouldLookUpDriverExactlyOnce() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        verify(driverRepository, times(1)).findById(DRIVER_ID);
    }

    @Test(description = "rideRepository.findByIdWithLock called exactly once")
    public void finishRide_shouldLockRideExactlyOnce() {
        Ride ride = buildInProgressRide();
        Driver driver = buildDriverWithRide(ride);
        stubHappyPath(driver, ride);

        driverService.finishRide(DRIVER_ID, NORMAL_END);

        verify(rideRepository, times(1)).findByIdWithLock(RIDE_ID);
    }
}