package com.st3.uber.s1;

import com.st3.uber.domain.Driver;
import com.st3.uber.domain.Passenger;
import com.st3.uber.domain.Ride;
import com.st3.uber.domain.RideInvite;
import com.st3.uber.dto.location.LocationRequest;
import com.st3.uber.dto.ride.CreateRideRequest;
import com.st3.uber.dto.route.RouteInfo;
import com.st3.uber.enums.NotificationType;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.enums.VehicleType;
import com.st3.uber.exception.UserBlockedException;
import com.st3.uber.repository.*;
import com.st3.uber.service.*;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

public class RideServiceTest {

    private RideRepository rideRepository;
    private PassengerRepository passengerRepository;
    private RideInviteRepository rideInviteRepository;
    private RouteCalculationService routeCalculationService;
    private PriceCalculationService priceCalculationService;
    private DriverService driverService;
    private RideInviteMailService rideInviteMailService;
    private NotificationService notificationService;
    private MailService mailService;
    private DriverRepository driverRepository;

    private RideService rideService;

    //Data for passenger
    private static final Long PASSENGER_ID = 1L;
    private static final String PASSENGER_EMAIL = "passenger@test.com";
    private static final String PASSENGER_NAME = "Marko";


    //Data for driver
    private static final Long DRIVER_ID = 10L;
    private static final String DRIVER_NAME = "Pera";
    private static final String DRIVER_SURNAME = "Peric";


    //Data for location
    private static final double LAT = 45.2671;
    private static final double LNG = 19.8335;
    private static final String ADDRESS = "Bulevar Oslobodjenja 1";


    //Data for price
    private static final double DISTANCE_KM = 5.0;
    private static final int DURATION_MIN = 10;
    private static final double BASE_PRICE = 150;
    private static final double CALCULATED_PRICE = 320;

    //Data for time
    private static final LocalDateTime VALID_TIME =
            LocalDateTime.now().plusHours(1);

    private static final LocalDateTime PAST_TIME =
            LocalDateTime.now().minusHours(1);

    private static final LocalDateTime FUTURE_TIME =
            LocalDateTime.now().plusHours(6);


    @BeforeMethod
    public void setup() {
        rideRepository = Mockito.mock(RideRepository.class);
        passengerRepository = Mockito.mock(PassengerRepository.class);
        rideInviteRepository = Mockito.mock(RideInviteRepository.class);
        routeCalculationService = Mockito.mock(RouteCalculationService.class);
        priceCalculationService = Mockito.mock(PriceCalculationService.class);
        driverService = Mockito.mock(DriverService.class);
        rideInviteMailService = Mockito.mock(RideInviteMailService.class);
        notificationService = Mockito.mock(NotificationService.class);
        mailService = Mockito.mock(MailService.class);
        driverRepository = Mockito.mock(DriverRepository.class);

        rideService = new RideService(
                rideRepository,
                passengerRepository,
                rideInviteRepository,
                routeCalculationService,
                priceCalculationService,
                driverService,
                rideInviteMailService,
                notificationService,
                mailService,
                driverRepository
        );
    }


    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWhenPassengerNotFound() {

        Mockito.when(passengerRepository.findById(PASSENGER_ID))
                .thenReturn(Optional.empty());

        CreateRideRequest request = Mockito.mock(CreateRideRequest.class);

        Ride createdRide = rideService.createRide(PASSENGER_ID, request);

        assertNull(createdRide);
        Mockito.verify(passengerRepository).findById(PASSENGER_ID);
    }


    @Test(expectedExceptions = UserBlockedException.class)
    public void testWhenPassengerIsBlocked() {

        //Setting test passenger
        Passenger passenger = new Passenger();
        passenger.setId(PASSENGER_ID);
        passenger.setEmail(PASSENGER_EMAIL);
        passenger.setName(PASSENGER_NAME);
        passenger.setBlocked(true);
        passenger.setBlockReason("Blocked by admin");

        Mockito.when(passengerRepository.findById(PASSENGER_ID)).thenReturn(Optional.of(passenger));
        CreateRideRequest request = Mockito.mock(CreateRideRequest.class);


        Ride createdRide = rideService.createRide(PASSENGER_ID,request);

        assertNull(createdRide);
        Mockito.verify(passengerRepository).findById(PASSENGER_ID);

    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testScheduledInPast() {
        //Setting test passenger
        Passenger passenger = new Passenger();
        passenger.setId(PASSENGER_ID);
        passenger.setEmail(PASSENGER_EMAIL);
        passenger.setName(PASSENGER_NAME);
        passenger.setBlocked(false);


        //Passenger is not blocked
        Mockito.when(passengerRepository.findById(PASSENGER_ID)).thenReturn(Optional.of(passenger));
        CreateRideRequest request = Mockito.mock(CreateRideRequest.class);

        //Passenger tries to schedule ride in past
        Mockito.when(request.scheduledAt()).thenReturn(PAST_TIME);

        Ride createdRide = rideService.createRide(PASSENGER_ID,request);

        assertNull(createdRide);
        Mockito.verify(passengerRepository).findById(PASSENGER_ID);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testScheduledAfterFiveHours() {
        //Setting test passenger
        Passenger passenger = new Passenger();
        passenger.setId(PASSENGER_ID);
        passenger.setEmail(PASSENGER_EMAIL);
        passenger.setName(PASSENGER_NAME);
        passenger.setBlocked(false);


        //Passenger is not blocked
        Mockito.when(passengerRepository.findById(PASSENGER_ID)).thenReturn(Optional.of(passenger));
        CreateRideRequest request = Mockito.mock(CreateRideRequest.class);

        //Passenger tries to schedule ride in future
        Mockito.when(request.scheduledAt()).thenReturn(FUTURE_TIME);

        Ride createdRide = rideService.createRide(PASSENGER_ID,request);

        assertNull(createdRide);
        Mockito.verify(passengerRepository).findById(PASSENGER_ID);
    }

    //ADD ALSO HAPPY PATH FOR NOT NULL SCHEDULED AT
    @Test(expectedExceptions = IllegalStateException.class)
    public void testHasActiveRide() {
        //Setting test passenger
        Passenger passenger = new Passenger();
        passenger.setId(PASSENGER_ID);
        passenger.setEmail(PASSENGER_EMAIL);
        passenger.setName(PASSENGER_NAME);
        passenger.setBlocked(false);


        //Passenger is not blocked
        Mockito.when(passengerRepository.findById(PASSENGER_ID)).thenReturn(Optional.of(passenger));
        CreateRideRequest request = Mockito.mock(CreateRideRequest.class);


        Mockito.when(request.scheduledAt()).thenReturn(null);

        //Return like the ride exists
        Mockito.when(rideRepository.existsByCreatorAndStatusIn(eq(passenger),Mockito.anyList())).thenReturn(true);

        Ride createdRide = rideService.createRide(PASSENGER_ID,request);

        assertNull(createdRide);
        InOrder inOrder = Mockito.inOrder(passengerRepository,rideRepository);
        inOrder.verify(passengerRepository).findById(PASSENGER_ID);
        inOrder.verify(rideRepository).existsByCreatorAndStatusIn(Mockito.eq(passenger), Mockito.anyList());
    }


    @Test(expectedExceptions = IllegalStateException.class)
    public void testDriversReservedForScheduledRides() {
        //Setting test passenger
        Passenger passenger = new Passenger();
        passenger.setId(PASSENGER_ID);
        passenger.setEmail(PASSENGER_EMAIL);
        passenger.setName(PASSENGER_NAME);
        passenger.setBlocked(false);


        //Passenger is not blocked
        Mockito.when(passengerRepository.findById(PASSENGER_ID)).thenReturn(Optional.of(passenger));

        LocationRequest start = Mockito.mock(LocationRequest.class);
        LocationRequest end = Mockito.mock(LocationRequest.class);


        //doesnt have scheduled ride
        CreateRideRequest request =
                new CreateRideRequest(
                        start,
                        end,
                        null, //for first test
                        null,
                        VehicleType.STANDARD,
                        false,
                        false,
                        null
                );


        //Doesnt have active ride
        Mockito.when(rideRepository.existsByCreatorAndStatusIn(eq(passenger),Mockito.anyList())).thenReturn(false);

        Mockito.when(rideRepository.existsByStatusAndScheduledAtBetween(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);

        Ride createdRide = rideService.createRide(PASSENGER_ID,request);


        assertNull(createdRide);
        InOrder inOrder = Mockito.inOrder(passengerRepository,rideRepository);

        inOrder.verify(passengerRepository).findById(PASSENGER_ID);
        inOrder.verify(rideRepository).existsByCreatorAndStatusIn(Mockito.eq(passenger), Mockito.anyList());
        inOrder.verify(rideRepository).existsByStatusAndScheduledAtBetween(Mockito.any(),Mockito.any(),Mockito.any());


    }



    //Test happy path case:
    // 1. No other passengers
    // 2. Not scheduled
    @Test(dataProvider = "validInstantRequests")
    public void testHappyPath1(CreateRideRequest request) {
        //Setting test passenger
        Passenger passenger = new Passenger();
        passenger.setId(PASSENGER_ID);
        passenger.setEmail(PASSENGER_EMAIL);
        passenger.setName(PASSENGER_NAME);
        passenger.setBlocked(false);


        //Passenger is not blocked
        Mockito.when(passengerRepository.findById(PASSENGER_ID)).thenReturn(Optional.of(passenger));



        //Doesnt have active ride
        Mockito.when(rideRepository.existsByCreatorAndStatusIn(eq(passenger),Mockito.anyList())).thenReturn(false);


        Mockito.when(rideRepository.existsByStatusAndScheduledAtBetween(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(false);


        //Mock driver
        Driver driver = new Driver();
        driver.setId(DRIVER_ID);
        driver.setName(DRIVER_NAME);
        driver.setSurname(DRIVER_SURNAME);

        Mockito.when(driverService.findDriverForRide(Mockito.any())).thenReturn(driver);


        //Route calculation
        RouteInfo routeInfo = new RouteInfo(DISTANCE_KM,DURATION_MIN);
        Mockito.when(routeCalculationService.calculateRoute(
                Mockito.any(),
                Mockito.any(),
                Mockito.anyList()
        )).thenReturn(routeInfo);

        //Getting base price
        Mockito.when(priceCalculationService.getBasePrice(Mockito.any())).thenReturn(BASE_PRICE);

        //Price calculation
        Mockito.when(priceCalculationService.calculatePrice(Mockito.any(), Mockito.anyDouble())).thenReturn(CALCULATED_PRICE);


        //Save ride
        Mockito.when(rideRepository.save(Mockito.any()))
                .thenAnswer(invocation -> {
                    Ride r = invocation.getArgument(0);
                    r.setId(100L);
                    return r;
                });

        Ride createdRide = rideService.createRide(PASSENGER_ID,request);

        assertNotNull(createdRide);
        InOrder inOrder = Mockito.inOrder(passengerRepository,rideRepository, driverService, routeCalculationService,
                priceCalculationService, notificationService);

        inOrder.verify(passengerRepository).findById(PASSENGER_ID);
        inOrder.verify(rideRepository).existsByCreatorAndStatusIn(Mockito.eq(passenger), Mockito.anyList());
        inOrder.verify(rideRepository).existsByStatusAndScheduledAtBetween(Mockito.any(),Mockito.any(),Mockito.any());
        inOrder.verify(driverService).findDriverForRide(Mockito.any());
        inOrder.verify(routeCalculationService).calculateRoute(Mockito.any(), Mockito.any(), Mockito.anyList());
        inOrder.verify(priceCalculationService).getBasePrice(Mockito.any());
        inOrder.verify(priceCalculationService).calculatePrice(Mockito.any(), Mockito.anyDouble());
        inOrder.verify(rideRepository).save(Mockito.any());
        inOrder.verify(notificationService).createNotification(eq(PASSENGER_ID), Mockito.anyString(),
                        eq(NotificationType.ACCEPTED_RIDE),
                        anyLong()
        );
        inOrder.verify(notificationService).createNotification(eq(DRIVER_ID), Mockito.anyString(),
                        eq(NotificationType.RIDE_REMINDER),
                        anyLong()
        );

    }


    @DataProvider
    public Object[][] validInstantRequests() {
        return new Object[][] {

                // no stops, no emails
                {
                        new CreateRideRequest(
                                new LocationRequest(LAT, LNG, ADDRESS),
                                new LocationRequest(LAT, LNG, ADDRESS),
                                null,
                                null,
                                VehicleType.STANDARD,
                                false,
                                false,
                                null
                        )
                },

                // with stops
                {
                        new CreateRideRequest(
                                new LocationRequest(LAT, LNG, ADDRESS),
                                new LocationRequest(LAT, LNG, ADDRESS),
                                List.of(new LocationRequest(LAT, LNG, "Stop1")),
                                List.of(), //Primer i ovako
                                VehicleType.STANDARD,
                                false,
                                false,
                                null
                        )
                }
        };
    }




    //Test happy path case:
    // 1. With passengers
    // 2. Not scheduled
    @Test(dataProvider = "validEmailRequests")
    public void testHappyPath2(CreateRideRequest request) {
        //Setting test passenger
        Passenger passenger = new Passenger();
        passenger.setId(PASSENGER_ID);
        passenger.setEmail(PASSENGER_EMAIL);
        passenger.setName(PASSENGER_NAME);
        passenger.setBlocked(false);


        //Passenger is not blocked
        Mockito.when(passengerRepository.findById(PASSENGER_ID)).thenReturn(Optional.of(passenger));



        //Doesnt have active ride
        Mockito.when(rideRepository.existsByCreatorAndStatusIn(eq(passenger),Mockito.anyList())).thenReturn(false);


        Mockito.when(passengerRepository.findByEmail(Mockito.anyString()))
                .thenAnswer(invocation -> {

                    String email = invocation.getArgument(0);

                    if(email.equals("a@gmail.com")) {
                        Passenger existing = new Passenger();
                        existing.setId(2L);
                        existing.setEmail(email);
                        return Optional.of(existing);
                    }

                    return Optional.empty();
                });



        Mockito.when(rideRepository.existsByStatusAndScheduledAtBetween(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(false);


        //Mock driver
        Driver driver = new Driver();
        driver.setId(DRIVER_ID);
        driver.setName(DRIVER_NAME);
        driver.setSurname(DRIVER_SURNAME);

        Mockito.when(driverService.findDriverForRide(Mockito.any())).thenReturn(driver);


        //Route calculation
        RouteInfo routeInfo = new RouteInfo(DISTANCE_KM,DURATION_MIN);
        Mockito.when(routeCalculationService.calculateRoute(
                Mockito.any(),
                Mockito.any(),
                Mockito.anyList()
        )).thenReturn(routeInfo);

        //Getting base price
        Mockito.when(priceCalculationService.getBasePrice(Mockito.any())).thenReturn(BASE_PRICE);

        //Price calculation
        Mockito.when(priceCalculationService.calculatePrice(Mockito.any(), Mockito.anyDouble())).thenReturn(CALCULATED_PRICE);


        //Save ride
        Mockito.when(rideRepository.save(Mockito.any()))
                .thenAnswer(invocation -> {
                    Ride r = invocation.getArgument(0);
                    r.setId(100L);
                    return r;
                });



        Ride createdRide = rideService.createRide(PASSENGER_ID,request);

        assertNotNull(createdRide);
        InOrder inOrder = Mockito.inOrder(passengerRepository, rideRepository, driverService, routeCalculationService,
                priceCalculationService, rideInviteMailService, notificationService);

        inOrder.verify(passengerRepository).findById(PASSENGER_ID);
        inOrder.verify(rideRepository).existsByCreatorAndStatusIn(Mockito.eq(passenger), Mockito.anyList());
        inOrder.verify(rideRepository).existsByStatusAndScheduledAtBetween(Mockito.any(),Mockito.any(),Mockito.any());
        inOrder.verify(driverService).findDriverForRide(Mockito.any());
        inOrder.verify(routeCalculationService).calculateRoute(Mockito.any(), Mockito.any(), Mockito.anyList());
        inOrder.verify(priceCalculationService).getBasePrice(Mockito.any());
        inOrder.verify(priceCalculationService).calculatePrice(Mockito.any(), Mockito.anyDouble());
        inOrder.verify(rideRepository).save(Mockito.any());
        inOrder.verify(rideInviteMailService).sendInvite(
                Mockito.argThat(inv -> inv.getEmail().equals("b@gmail.com")),
                Mockito.eq(passenger),
                Mockito.any()
        );
        inOrder.verify(notificationService).createNotification(eq(PASSENGER_ID), Mockito.anyString(),
                eq(NotificationType.ACCEPTED_RIDE),
                anyLong()
        );
        inOrder.verify(notificationService).createNotification(eq(DRIVER_ID), Mockito.anyString(),
                eq(NotificationType.RIDE_REMINDER),
                anyLong()
        );





    }


    @DataProvider
    public Object[][] validEmailRequests() {
        return new Object[][] {

                //with emails
                {
                        new CreateRideRequest(
                                new LocationRequest(LAT, LNG, ADDRESS),
                                new LocationRequest(LAT, LNG, ADDRESS),
                                null,
                                List.of("a@gmail.com", "b@gmail.com"),
                                VehicleType.STANDARD,
                                false,
                                false,
                                null
                        )
                },

                // with stops with emails
                {
                        new CreateRideRequest(
                                new LocationRequest(LAT, LNG, ADDRESS),
                                new LocationRequest(LAT, LNG, ADDRESS),
                                List.of(new LocationRequest(LAT, LNG, "Stop1")),
                                List.of("a@gmail.com", "b@gmail.com"),
                                VehicleType.STANDARD,
                                false,
                                false,
                                null
                        )
                }
        };
    }




    //Test happy path case:
    // 1. With passengers
    // 2. Scheduled

    @Test(dataProvider = "validScheduledEmailRequests")
    public void testHappyPath3(CreateRideRequest request) {
        //Setting test passenger
        Passenger passenger = new Passenger();
        passenger.setId(PASSENGER_ID);
        passenger.setEmail(PASSENGER_EMAIL);
        passenger.setName(PASSENGER_NAME);
        passenger.setBlocked(false);


        //Passenger is not blocked
        Mockito.when(passengerRepository.findById(PASSENGER_ID)).thenReturn(Optional.of(passenger));



        //Doesnt have active ride
        Mockito.when(rideRepository.existsByCreatorAndStatusIn(eq(passenger),Mockito.anyList())).thenReturn(false);


        Mockito.when(passengerRepository.findByEmail(Mockito.anyString()))
                .thenAnswer(invocation -> {

                    String email = invocation.getArgument(0);

                    if(email.equals("a@gmail.com")) {
                        Passenger existing = new Passenger();
                        existing.setId(2L);
                        existing.setEmail(email);
                        return Optional.of(existing);
                    }

                    return Optional.empty();
                });


        //Route calculation
        RouteInfo routeInfo = new RouteInfo(DISTANCE_KM,DURATION_MIN);
        Mockito.when(routeCalculationService.calculateRoute(
                Mockito.any(),
                Mockito.any(),
                Mockito.anyList()
        )).thenReturn(routeInfo);

        //Getting base price
        Mockito.when(priceCalculationService.getBasePrice(Mockito.any())).thenReturn(BASE_PRICE);

        //Price calculation
        Mockito.when(priceCalculationService.calculatePrice(Mockito.any(), Mockito.anyDouble())).thenReturn(CALCULATED_PRICE);


        //Save ride
        Mockito.when(rideRepository.save(Mockito.any()))
                .thenAnswer(invocation -> {
                    Ride r = invocation.getArgument(0);
                    r.setId(100L);
                    return r;
                });



        Ride createdRide = rideService.createRide(PASSENGER_ID, request);

        assertNotNull(createdRide);
        assertEquals(createdRide.getStatus(), RideStatus.PENDING);

        InOrder inOrder = Mockito.inOrder(
                passengerRepository,
                rideRepository,
                routeCalculationService,
                priceCalculationService,
                rideInviteMailService,
                notificationService
        );

        inOrder.verify(passengerRepository).findById(PASSENGER_ID);
        inOrder.verify(rideRepository)
                .existsByCreatorAndStatusIn(eq(passenger), Mockito.anyList());

        Mockito.verify(rideRepository, Mockito.never())
                .existsByStatusAndScheduledAtBetween(Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.verify(driverService, Mockito.never())
                .findDriverForRide(Mockito.any());

        inOrder.verify(routeCalculationService)
                .calculateRoute(Mockito.any(), Mockito.any(), Mockito.anyList());

        inOrder.verify(priceCalculationService)
                .getBasePrice(Mockito.any());

        inOrder.verify(priceCalculationService)
                .calculatePrice(Mockito.any(), Mockito.anyDouble());

        inOrder.verify(rideRepository).save(Mockito.any());

        inOrder.verify(rideInviteMailService).sendInvite(
                Mockito.argThat(inv -> inv.getEmail().equals("b@gmail.com")),
                eq(passenger),
                Mockito.any()
        );

        inOrder.verify(notificationService).createNotification(
                eq(PASSENGER_ID),
                eq("Your ride has been scheduled successfully."),
                eq(NotificationType.RIDE_REMINDER),
                anyLong()
        );

        Mockito.verify(notificationService, Mockito.never())
                .createNotification(eq(DRIVER_ID), Mockito.anyString(), Mockito.any(), anyLong());
    }

    @DataProvider
    public Object[][] validScheduledEmailRequests() {
        return new Object[][] {

                // with emails
                {
                        new CreateRideRequest(
                                new LocationRequest(LAT, LNG, ADDRESS),
                                new LocationRequest(LAT, LNG, ADDRESS),
                                null,
                                List.of("a@gmail.com", "b@gmail.com"),
                                VehicleType.STANDARD,
                                false,
                                false,
                                LocalDateTime.now().plusHours(1)
                        )
                },

                // with stops + emails
                {
                        new CreateRideRequest(
                                new LocationRequest(LAT, LNG, ADDRESS),
                                new LocationRequest(LAT, LNG, ADDRESS),
                                List.of(new LocationRequest(LAT, LNG, "Stop1")),
                                List.of("a@gmail.com", "b@gmail.com"),
                                VehicleType.STANDARD,
                                false,
                                false,
                                LocalDateTime.now().plusHours(2)
                        )
                }
        };
    }


    //Test happy path case:
    // 1. No other passengers
    // 2. Scheduled
    @Test(dataProvider = "validScheduledNoEmailRequests")
    public void testScheduledHappyPathNoEmails(CreateRideRequest request) {

        // Passenger
        Passenger passenger = new Passenger();
        passenger.setId(PASSENGER_ID);
        passenger.setEmail(PASSENGER_EMAIL);
        passenger.setName(PASSENGER_NAME);
        passenger.setBlocked(false);

        Mockito.when(passengerRepository.findById(PASSENGER_ID))
                .thenReturn(Optional.of(passenger));

        Mockito.when(rideRepository.existsByCreatorAndStatusIn(
                        Mockito.eq(passenger),
                        Mockito.anyList()))
                .thenReturn(false);

        RouteInfo routeInfo = new RouteInfo(DISTANCE_KM, DURATION_MIN);

        Mockito.when(routeCalculationService.calculateRoute(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.anyList()))
                .thenReturn(routeInfo);

        Mockito.when(priceCalculationService.getBasePrice(Mockito.any()))
                .thenReturn(BASE_PRICE);

        Mockito.when(priceCalculationService.calculatePrice(
                        Mockito.any(),
                        Mockito.anyDouble()))
                .thenReturn(CALCULATED_PRICE);

        Mockito.when(rideRepository.save(Mockito.any()))
                .thenAnswer(invocation -> {
                    Ride r = invocation.getArgument(0);
                    r.setId(100L);
                    return r;
                });

        Ride createdRide = rideService.createRide(PASSENGER_ID, request);

        assertNotNull(createdRide);
        assertEquals(createdRide.getStatus(), RideStatus.PENDING);

        InOrder inOrder = Mockito.inOrder(
                passengerRepository,
                rideRepository,
                routeCalculationService,
                priceCalculationService,
                notificationService
        );

        inOrder.verify(passengerRepository)
                .findById(PASSENGER_ID);

        inOrder.verify(rideRepository)
                .existsByCreatorAndStatusIn(
                        Mockito.eq(passenger),
                        Mockito.anyList()
                );

        Mockito.verify(rideRepository, Mockito.never())
                .existsByStatusAndScheduledAtBetween(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any()
                );

        Mockito.verify(driverService, Mockito.never())
                .findDriverForRide(Mockito.any());

        inOrder.verify(routeCalculationService)
                .calculateRoute(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.anyList()
                );

        inOrder.verify(priceCalculationService)
                .getBasePrice(Mockito.any());

        inOrder.verify(priceCalculationService)
                .calculatePrice(
                        Mockito.any(),
                        Mockito.anyDouble()
                );

        inOrder.verify(rideRepository)
                .save(Mockito.any());

        Mockito.verify(rideInviteMailService, Mockito.never())
                .sendInvite(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any()
                );

        inOrder.verify(notificationService)
                .createNotification(
                        Mockito.eq(PASSENGER_ID),
                        Mockito.eq("Your ride has been scheduled successfully."),
                        Mockito.eq(NotificationType.RIDE_REMINDER),
                        Mockito.anyLong()
                );

        Mockito.verify(notificationService, Mockito.never())
                .createNotification(
                        Mockito.eq(DRIVER_ID),
                        Mockito.anyString(),
                        Mockito.any(),
                        Mockito.anyLong()
                );
    }


    @DataProvider
    public Object[][] validScheduledNoEmailRequests() {
        return new Object[][] {

                // no emails, no stops
                {
                        new CreateRideRequest(
                                new LocationRequest(LAT, LNG, ADDRESS),
                                new LocationRequest(LAT, LNG, ADDRESS),
                                null,
                                null,
                                VehicleType.STANDARD,
                                false,
                                false,
                                LocalDateTime.now().plusHours(1)
                        )
                },

                // stops but no emails
                {
                        new CreateRideRequest(
                                new LocationRequest(LAT, LNG, ADDRESS),
                                new LocationRequest(LAT, LNG, ADDRESS),
                                List.of(new LocationRequest(LAT, LNG, "Stop1")),
                                List.of(),
                                VehicleType.STANDARD,
                                false,
                                false,
                                LocalDateTime.now().plusHours(2)
                        )
                }
        };
    }


    //email.equalsIgnoreCase(creator.getEmail()) case
    @Test
    public void testCreatorEmailIgnoredInPassengerEmails() {

        Passenger creator = new Passenger();
        creator.setId(PASSENGER_ID);
        creator.setEmail(PASSENGER_EMAIL);
        creator.setName(PASSENGER_NAME);
        creator.setBlocked(false);

        Mockito.when(passengerRepository.findById(PASSENGER_ID))
                .thenReturn(Optional.of(creator));

        Mockito.when(rideRepository.existsByCreatorAndStatusIn(
                Mockito.eq(creator),
                Mockito.anyList()
        )).thenReturn(false);

        Mockito.when(rideRepository.existsByStatusAndScheduledAtBetween(
                Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(false);

        Driver driver = new Driver();
        driver.setId(DRIVER_ID);
        driver.setName(DRIVER_NAME);
        driver.setSurname(DRIVER_SURNAME);

        Mockito.when(driverService.findDriverForRide(Mockito.any()))
                .thenReturn(driver);

        RouteInfo routeInfo = new RouteInfo(DISTANCE_KM, DURATION_MIN);

        Mockito.when(routeCalculationService.calculateRoute(
                Mockito.any(), Mockito.any(), Mockito.anyList()
        )).thenReturn(routeInfo);

        Mockito.when(priceCalculationService.getBasePrice(Mockito.any()))
                .thenReturn(BASE_PRICE);

        Mockito.when(priceCalculationService.calculatePrice(
                Mockito.any(), Mockito.anyDouble()
        )).thenReturn(CALCULATED_PRICE);

        Mockito.when(rideRepository.save(Mockito.any()))
                .thenAnswer(inv -> {
                    Ride r = inv.getArgument(0);
                    r.setId(100L);
                    return r;
                });

        // Creator email inside list
        CreateRideRequest request = new CreateRideRequest(
                new LocationRequest(LAT, LNG, ADDRESS),
                new LocationRequest(LAT, LNG, ADDRESS),
                null,
                List.of(PASSENGER_EMAIL), // SAME EMAIL
                VehicleType.STANDARD,
                false,
                false,
                null
        );

        Ride ride = rideService.createRide(PASSENGER_ID, request);

        assertNotNull(ride);

        // Creator must be only passenger
        assertEquals(ride.getPassengers().size(), 1);

        // No invites created
        assertEquals(ride.getInvites().size(), 0);

        Mockito.verify(rideInviteMailService, Mockito.never())
                .sendInvite(Mockito.any(), Mockito.any(), Mockito.any());

        // Only normal creator notification (accepted ride)
        Mockito.verify(notificationService, Mockito.times(1))
                .createNotification(
                        Mockito.eq(PASSENGER_ID),
                        Mockito.anyString(),
                        Mockito.eq(NotificationType.ACCEPTED_RIDE),
                        Mockito.anyLong()
                );
    }


    //duplicate emails
    @Test
    public void testDuplicateEmailsAddPassengerOnlyOnce() {

        Passenger creator = new Passenger();
        creator.setId(PASSENGER_ID);
        creator.setEmail(PASSENGER_EMAIL);
        creator.setName(PASSENGER_NAME);
        creator.setBlocked(false);

        Mockito.when(passengerRepository.findById(PASSENGER_ID))
                .thenReturn(Optional.of(creator));

        Mockito.when(rideRepository.existsByCreatorAndStatusIn(
                Mockito.eq(creator),
                Mockito.anyList()
        )).thenReturn(false);

        Mockito.when(rideRepository.existsByStatusAndScheduledAtBetween(
                Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(false);

        // Existing passenger
        Passenger invited = new Passenger();
        invited.setId(2L);
        invited.setEmail("a@gmail.com");

        Mockito.when(passengerRepository.findByEmail("a@gmail.com"))
                .thenReturn(Optional.of(invited));

        Driver driver = new Driver();
        driver.setId(DRIVER_ID);
        driver.setName(DRIVER_NAME);
        driver.setSurname(DRIVER_SURNAME);

        Mockito.when(driverService.findDriverForRide(Mockito.any()))
                .thenReturn(driver);

        RouteInfo routeInfo = new RouteInfo(DISTANCE_KM, DURATION_MIN);

        Mockito.when(routeCalculationService.calculateRoute(
                Mockito.any(), Mockito.any(), Mockito.anyList()
        )).thenReturn(routeInfo);

        Mockito.when(priceCalculationService.getBasePrice(Mockito.any()))
                .thenReturn(BASE_PRICE);

        Mockito.when(priceCalculationService.calculatePrice(
                Mockito.any(), Mockito.anyDouble()
        )).thenReturn(CALCULATED_PRICE);

        Mockito.when(rideRepository.save(Mockito.any()))
                .thenAnswer(inv -> {
                    Ride r = inv.getArgument(0);
                    r.setId(100L);
                    return r;
                });

        CreateRideRequest request = new CreateRideRequest(
                new LocationRequest(LAT, LNG, ADDRESS),
                new LocationRequest(LAT, LNG, ADDRESS),
                null,
                List.of("a@gmail.com", "a@gmail.com"), // DUPLICATES
                VehicleType.STANDARD,
                false,
                false,
                null
        );

        Ride ride = rideService.createRide(PASSENGER_ID, request);

        assertNotNull(ride);

        // Creator + 1 invited
        assertEquals(ride.getPassengers().size(), 2);

        // Notification for invited passenger only ONCE
        Mockito.verify(notificationService, Mockito.times(1))
                .createNotification(
                        Mockito.eq(2L),
                        Mockito.anyString(),
                        Mockito.eq(NotificationType.RIDE_REMINDER),
                        Mockito.anyLong()
                );
    }


    @Test
    public void testInviteCreatedForUnknownEmail() {

        Passenger creator = new Passenger();
        creator.setId(PASSENGER_ID);
        creator.setEmail(PASSENGER_EMAIL);
        creator.setName(PASSENGER_NAME);
        creator.setBlocked(false);

        Mockito.when(passengerRepository.findById(PASSENGER_ID))
                .thenReturn(Optional.of(creator));

        Mockito.when(rideRepository.existsByCreatorAndStatusIn(
                Mockito.eq(creator),
                Mockito.anyList()
        )).thenReturn(false);

        Mockito.when(rideRepository.existsByStatusAndScheduledAtBetween(
                Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(false);

        // Email NOT found
        Mockito.when(passengerRepository.findByEmail("ghost@gmail.com"))
                .thenReturn(Optional.empty());

        Driver driver = new Driver();
        driver.setId(DRIVER_ID);

        Mockito.when(driverService.findDriverForRide(Mockito.any()))
                .thenReturn(driver);

        RouteInfo routeInfo = new RouteInfo(DISTANCE_KM, DURATION_MIN);

        Mockito.when(routeCalculationService.calculateRoute(
                Mockito.any(), Mockito.any(), Mockito.anyList()
        )).thenReturn(routeInfo);

        Mockito.when(priceCalculationService.getBasePrice(Mockito.any()))
                .thenReturn(BASE_PRICE);

        Mockito.when(priceCalculationService.calculatePrice(
                Mockito.any(), Mockito.anyDouble()
        )).thenReturn(CALCULATED_PRICE);

        Mockito.when(rideRepository.save(Mockito.any()))
                .thenAnswer(inv -> {
                    Ride r = inv.getArgument(0);
                    r.setId(100L);
                    return r;
                });

        CreateRideRequest request = new CreateRideRequest(
                new LocationRequest(LAT, LNG, ADDRESS),
                new LocationRequest(LAT, LNG, ADDRESS),
                null,
                List.of("ghost@gmail.com"),
                VehicleType.STANDARD,
                false,
                false,
                null
        );

        Ride ride = rideService.createRide(PASSENGER_ID, request);

        assertNotNull(ride);

        // Invite created
        assertEquals(ride.getInvites().size(), 1);

        RideInvite invite = ride.getInvites().get(0);

        assertEquals(invite.getEmail(), "ghost@gmail.com");
        assertNotNull(invite.getTrackingToken());
        assertNotNull(invite.getCreatedAt());

        Mockito.verify(rideInviteMailService, Mockito.times(1))
                .sendInvite(Mockito.any(), Mockito.eq(creator), Mockito.any());
    }

    @Test
    public void testDriverStateUpdatedForInstantRide() {

        Passenger creator = new Passenger();
        creator.setId(PASSENGER_ID);
        creator.setEmail(PASSENGER_EMAIL);
        creator.setName(PASSENGER_NAME);
        creator.setBlocked(false);

        Mockito.when(passengerRepository.findById(PASSENGER_ID))
                .thenReturn(Optional.of(creator));

        Mockito.when(rideRepository.existsByCreatorAndStatusIn(
                Mockito.eq(creator),
                Mockito.anyList()
        )).thenReturn(false);

        Mockito.when(rideRepository.existsByStatusAndScheduledAtBetween(
                Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(false);

        // Driver
        Driver driver = new Driver();
        driver.setId(DRIVER_ID);
        driver.setFree(true);
        driver.setAvailable(true);

        Mockito.when(driverService.findDriverForRide(Mockito.any()))
                .thenReturn(driver);

        RouteInfo routeInfo = new RouteInfo(DISTANCE_KM, DURATION_MIN);

        Mockito.when(routeCalculationService.calculateRoute(
                Mockito.any(), Mockito.any(), Mockito.anyList()
        )).thenReturn(routeInfo);

        Mockito.when(priceCalculationService.getBasePrice(Mockito.any()))
                .thenReturn(BASE_PRICE);

        Mockito.when(priceCalculationService.calculatePrice(
                Mockito.any(), Mockito.anyDouble()
        )).thenReturn(CALCULATED_PRICE);

        Mockito.when(rideRepository.save(Mockito.any()))
                .thenAnswer(inv -> {
                    Ride r = inv.getArgument(0);
                    r.setId(100L);
                    return r;
                });

        CreateRideRequest request = new CreateRideRequest(
                new LocationRequest(LAT, LNG, ADDRESS),
                new LocationRequest(LAT, LNG, ADDRESS),
                null,
                null,
                VehicleType.STANDARD,
                false,
                false,
                null
        );

        Ride ride = rideService.createRide(PASSENGER_ID, request);

        assertNotNull(ride);

        // 🔥 STATE ASSERTIONS
        assertFalse(driver.isFree());
        assertFalse(driver.isAvailable());
        assertEquals(driver.getCurrentRide(), ride);
    }


}
