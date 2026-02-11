    package com.st3.uber.service;

    import com.st3.uber.domain.*;
    import com.st3.uber.dto.ride.*;
    import com.st3.uber.dto.route.RouteInfo;
    import com.st3.uber.enums.CancelledBy;
    import com.st3.uber.enums.NotificationType;
    import com.st3.uber.enums.RideStatus;
    import com.st3.uber.repository.DriverRepository;
    import com.st3.uber.exception.UserBlockedException;
    import com.st3.uber.repository.PassengerRepository;
    import com.st3.uber.repository.RideInviteRepository;
    import com.st3.uber.repository.RideRepository;
    import org.springframework.scheduling.annotation.Scheduled;
    import org.springframework.stereotype.Service;
    import com.st3.uber.dto.route.RouteEstimateRequest;
    import com.st3.uber.dto.route.RouteEstimateResponse;
    import com.st3.uber.dto.location.LocationRequest;
    import org.springframework.transaction.annotation.Propagation;
    import org.springframework.transaction.annotation.Transactional;
    import com.st3.uber.dto.report.RideReportResponse;
    import com.st3.uber.dto.report.DailyReportItem;
    import java.time.LocalDate;


    import java.util.*;

    import java.time.LocalDateTime;
    import java.util.stream.Collectors;

    @Service
    public class RideService {

        private final RideRepository rideRepository;
        private final PassengerRepository passengerRepository;
        private final RideInviteRepository rideInviteRepository;
        private final RouteCalculationService routeCalculationService;
        private final PriceCalculationService priceCalculationService;
        private final DriverService driverService;
        private final RideInviteMailService rideInviteMailService;
        private final NotificationService notificationService;
        private final MailService mailService;
        private final DriverRepository driverRepository;
        public RideService(
                RideRepository rideRepository,
                PassengerRepository passengerRepository,
                RideInviteRepository rideInviteRepository,
                RouteCalculationService routeCalculationService,
                PriceCalculationService priceCalculationService,
                DriverService driverService,
                RideInviteMailService rideInviteMailService,
                NotificationService notificationService,
                MailService mailService, DriverRepository driverRepository) {
            this.rideRepository = rideRepository;
            this.passengerRepository = passengerRepository;
            this.rideInviteRepository = rideInviteRepository;
            this.routeCalculationService = routeCalculationService;
            this.priceCalculationService = priceCalculationService;
            this.driverService = driverService;
            this.rideInviteMailService = rideInviteMailService;
            this.notificationService = notificationService;
            this.driverRepository = driverRepository;
            this.mailService = mailService;
        }

        @Transactional
        public Ride startRide(Long rideId) {
            Ride ride = rideRepository.findById(rideId)
                    .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

            if (ride.getStatus() != RideStatus.ACCEPTED) {
                throw new IllegalStateException("Ride must be ACCEPTED to start");
            }

            ride.setStatus(RideStatus.IN_PROGRESS);
            ride.setStartedAt(LocalDateTime.now());

            Ride savedRide = rideRepository.save(ride);

            for (Passenger passenger : savedRide.getPassengers()) {
                notificationService.createNotification(
                        passenger.getId(),
                        "Your ride is now in progress!",
                        NotificationType.RIDE_REMINDER,
                        savedRide.getId()
                );
            }

            return savedRide;
        }















        public Ride createRide(Long passengerId, CreateRideRequest request) {
            Passenger creator = passengerRepository.findById(passengerId)
                    .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));


            if (creator.isBlocked()) {
                throw new UserBlockedException(
                        creator.getBlockReason() != null
                                ? creator.getBlockReason()
                                : "Your account is blocked."
                );
            }



            LocalDateTime now = LocalDateTime.now();

            if (request.scheduledAt() != null) {

                if (request.scheduledAt().isBefore(now)) {
                    throw new IllegalStateException("Scheduled time cannot be in the past");
                }

                if (request.scheduledAt().isAfter(now.plusHours(5))) {
                    throw new IllegalStateException("Ride can be scheduled at most 5 hours ahead");
                }
            }

            if (rideRepository.existsByCreatorAndStatusIn(
                    creator,
                    List.of(RideStatus.PENDING, RideStatus.IN_PROGRESS, RideStatus.ACCEPTED)
            )) {
                throw new IllegalStateException("You already have an active or pending ride");
            }



            Ride ride = new Ride();
            ride.setCreator(creator);
            ride.getPassengers().add(creator);

            ride.setStartLocation(new Location(
                    request.startLocation().latitude(),
                    request.startLocation().longitude(),
                    request.startLocation().address()
            ));

            ride.setEndLocation(new Location(
                    request.endLocation().latitude(),
                    request.endLocation().longitude(),
                    request.endLocation().address()
            ));

            if (request.stops() != null && !request.stops().isEmpty()) {
                request.stops().forEach(s ->
                        ride.getRideStops().add(
                                new Location(
                                        s.latitude(),
                                        s.longitude(),
                                        s.address()
                                )
                        )
                );
            }

            List<Passenger> newlyAddedPassengers = new ArrayList<>();

            if (request.passengerEmails() != null && !request.passengerEmails().isEmpty()) {
                for (String email : request.passengerEmails()) {
                    if (email.equalsIgnoreCase(creator.getEmail())) {
                        continue;
                    }

                    passengerRepository.findByEmail(email)
                            .ifPresentOrElse(
                                    passenger -> {
                                        if (!ride.getPassengers().contains(passenger)) {
                                            ride.getPassengers().add(passenger);
                                            newlyAddedPassengers.add(passenger);
                                        }
                                    },

                                    () -> {
                                        RideInvite invite = new RideInvite();
                                        invite.setRide(ride);
                                        invite.setEmail(email);
                                        invite.setTrackingToken(UUID.randomUUID().toString());
                                        invite.setCreatedAt(LocalDateTime.now());
                                        ride.getInvites().add(invite);
                                    }
                            );
                }
            }

            ride.setVehicleType(request.vehicleType());
            ride.setBabyTransport(request.babyTransport());
            ride.setPetTransport(request.petTransport());

            Driver driver = null;

            if (request.scheduledAt() == null) {

                // PRIORITY CHECK FOR SCHEDULED RIDES
                boolean scheduledSoon = rideRepository
                        .existsByStatusAndScheduledAtBetween(
                                RideStatus.PENDING,
                                LocalDateTime.now(),
                                LocalDateTime.now().plusMinutes(30)
                        );

                if (scheduledSoon) {
                    throw new IllegalStateException(
                            "Drivers reserved for scheduled rides. Try again shortly."
                    );
                }

                // instant ride → assign driver now
                driver = driverService.findDriverForRide(ride);

                ride.setDriver(driver);
                driver.setCurrentRide(ride);
                driver.setFree(false);
                driver.setAvailable(false);

                ride.setStatus(RideStatus.ACCEPTED);
            }
            else {
                // scheduled ride
                ride.setStatus(RideStatus.PENDING);
            }


            RouteInfo routeInfo = routeCalculationService.calculateRoute(
                    ride.getStartLocation(),
                    ride.getEndLocation(),
                    ride.getRideStops()
            );

            ride.setDistance(routeInfo.distanceKm());
            ride.setEstimatedTimeMinutes(routeInfo.durationMinutes());

            double basePrice = priceCalculationService.getBasePrice(ride.getVehicleType());
            double calculatedPrice = priceCalculationService.calculatePrice(
                    ride.getVehicleType(),
                    routeInfo.distanceKm()
            );

            ride.setBasePrice(basePrice);
            ride.setCalculatedPrice(calculatedPrice);
            ride.setCreatedAt(LocalDateTime.now());
            ride.setScheduledAt(request.scheduledAt());
            Ride savedRide = rideRepository.save(ride);

            for (RideInvite invite : savedRide.getInvites()) {
                rideInviteMailService.sendInvite(invite, creator, savedRide);
            }

            for (Passenger p : newlyAddedPassengers) {
                notificationService.createNotification(
                        p.getId(),
                        String.format("%s invited you to a ride",
                                creator.getName()),
                        NotificationType.RIDE_REMINDER,
                        savedRide.getId()
                );

            }




            if (driver != null) {
                notificationService.createNotification(
                        creator.getId(),
                        String.format("Your ride request has been created. Driver %s %s will pick you up soon!",
                                driver.getName(), driver.getSurname()),
                        NotificationType.ACCEPTED_RIDE,
                        savedRide.getId()
                );
            } else {
                notificationService.createNotification(
                        creator.getId(),
                        "Your ride has been scheduled successfully.",
                        NotificationType.RIDE_REMINDER,
                        savedRide.getId()
                );
            }


            if(driver != null) {
                notificationService.createNotification(
                        driver.getId(),
                        String.format("New ride assigned! Pickup at %s",
                                savedRide.getStartLocation().getAddress()),
                        NotificationType.RIDE_REMINDER,
                        savedRide.getId()
                );

            }
            return savedRide;
        }



















        public RouteEstimateResponse estimateRoute(RouteEstimateRequest request) {
            Location start = new Location(
                    request.getStartLocation().latitude(),
                    request.getStartLocation().longitude(),
                    request.getStartLocation().address()
            );

            Location end = new Location(
                    request.getEndLocation().latitude(),
                    request.getEndLocation().longitude(),
                    request.getEndLocation().address()
            );

            List<Location> stops = new ArrayList<>();
            if (request.getStops() != null) {
                for (LocationRequest s : request.getStops()) {
                    stops.add(new Location(
                            s.latitude(),
                            s.longitude(),
                            s.address()
                    ));
                }
            }

            RouteInfo routeInfo = routeCalculationService.calculateRoute(
                    start, end, stops
            );

            double calculatedPrice = priceCalculationService.calculatePrice(
                    request.getVehicleType(),
                    routeInfo.distanceKm()
            );

            return new RouteEstimateResponse(
                    routeInfo.distanceKm(),
                    routeInfo.durationMinutes(),
                    calculatedPrice
            );
        }

        @Transactional(propagation = Propagation.MANDATORY)
        public Ride finishRideWithDetails(Long rideId, Location actualEndLocation) {
            Ride ride = rideRepository.findByIdWithLock(rideId)
                    .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

            if (ride.getStatus() != RideStatus.IN_PROGRESS) {
                throw new IllegalStateException("Only in-progress rides can be finished");
            }

            if (actualEndLocation != null) {
                ride.setActualEndLocation(actualEndLocation);
            } else {
                ride.setActualEndLocation(ride.getEndLocation());
            }

            if (ride.getActualRideStops().isEmpty() && !ride.getRideStops().isEmpty()) {
                ride.getActualRideStops().addAll(ride.getRideStops());
            }

            ride.setStatus(RideStatus.COMPLETED);
            ride.setFinishedAt(LocalDateTime.now());

           Ride savedRide = rideRepository.saveAndFlush(ride);


            for (Passenger passenger : savedRide.getPassengers()) {
                notificationService.createNotification(
                        passenger.getId(),
                        String.format("Your ride is complete! Total cost: %.2f RSD. Please rate your experience.",
                                savedRide.getCalculatedPrice()),
                        NotificationType.FINISHED_RIDE,
                        savedRide.getId()
                );
            }

            for (Passenger passenger : ride.getPassengers()) {
                try {
                    String subject = "Ride Completed";
                    String body = String.format("""
                Dear %s,
                
                Your ride has been completed.
                
                Ride Details:
                - From: %s
                - To: %s
                - Distance: %.2f km
                - Duration: %d minutes
                - Total Cost: %.2f RSD
                
                Thank you for riding with us!
                
                Best regards,
                Uber Team
                """,
                            passenger.getName(),
                            ride.getStartLocation().getAddress(),
                            ride.getActualEndLocation() != null ? ride.getActualEndLocation().getAddress() : ride.getEndLocation().getAddress(),
                            ride.getDistance(),
                            ride.getEstimatedTimeMinutes(),
                            ride.getCalculatedPrice()
                    );
                    mailService.sendText(passenger.getEmail(), subject, body);
                } catch (Exception e) {
                    System.err.println("Failed to send completion email to passenger: " + e.getMessage());
                }
            }

            for (RideInvite invite : ride.getInvites()) {
                try {
                    String subject = "Ride Completed - Tracking Update";
                    String body = String.format("""
                Hello,
                
                The ride you were tracking has been completed.
                
                Ride Details:
                - From: %s
                - To: %s
                - Distance: %.2f km
                
                Thank you for your interest!
                
                Best regards,
                Uber Team
                """,
                            ride.getStartLocation().getAddress(),
                            ride.getActualEndLocation() != null ? ride.getActualEndLocation().getAddress() : ride.getEndLocation().getAddress(),
                            ride.getDistance()
                    );
                    mailService.sendText(invite.getEmail(), subject, body);
                } catch (Exception e) {
                    System.err.println("Failed to send completion email to invite: " + e.getMessage());
                }
            }

            return savedRide;
        }

        public Ride reachStop(Long rideId, int stopIndex) {
            Ride ride = rideRepository.findById(rideId)
                    .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

            if (ride.getStatus() != RideStatus.IN_PROGRESS) {
                throw new IllegalStateException("Can only mark stops during active ride");
            }

            if (stopIndex < 0 || stopIndex >= ride.getRideStops().size()) {
                throw new IllegalArgumentException("Invalid stop index");
            }

            Location plannedStop = ride.getRideStops().get(stopIndex);

            boolean alreadyReached = ride.getActualRideStops().stream()
                    .anyMatch(s -> s.getLat().equals(plannedStop.getLat())
                            && s.getLng().equals(plannedStop.getLng()));

            if (!alreadyReached) {
                ride.getActualRideStops().add(plannedStop);
            }

            return rideRepository.save(ride);
        }

        public Ride acceptRide(Long rideId, Driver driver) {
            Ride ride = rideRepository.findById(rideId)
                    .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

            if (ride.getDriver() != null) {
                throw new IllegalStateException("Ride already has a driver assigned");
            }

            if (ride.getStatus() != RideStatus.PENDING) {
                throw new IllegalStateException("Can only accept pending rides");
            }

            ride.setDriver(driver);
            ride.setStatus(RideStatus.ACCEPTED);

            Ride savedRide = rideRepository.save(ride);

            for (Passenger passenger : savedRide.getPassengers()) {
                notificationService.createNotification(
                        passenger.getId(),
                        String.format("Driver %s %s has accepted your ride! They will arrive soon.",
                                driver.getName(), driver.getSurname()),
                        NotificationType.ACCEPTED_RIDE,
                        savedRide.getId()
                );
            }

            notificationService.createNotification(
                    driver.getId(),
                    String.format("You've accepted a ride. Pickup at %s",
                            savedRide.getStartLocation().getAddress()),
                    NotificationType.RIDE_REMINDER,
                    savedRide.getId()
            );

            return savedRide;
        }


        public void cancelRideByPassenger(Long rideId, Long passengerId){
            Passenger passenger = passengerRepository.findById(passengerId)
                    .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));
            Ride ride = rideRepository.findById(rideId)
                    .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

            if(ride.getDriver() != null){
                Driver driver = ride.getDriver();
                driver.setActive(true);
                driver.setAvailable(true);
                driver.setFree(true);
                driver.setCurrentRide(null);
                driverRepository.save(driver);
            }
            if (!ride.getCreator().equals(passenger))
                return;

            ride.setStatus(RideStatus.CANCELLED_BY_PASSENGER);
            ride.setCancelledAt(LocalDateTime.now());
            ride.setCancelledBy(CancelledBy.PASSENGER);

            rideRepository.save(ride);
        }

        @Scheduled(fixedRate = 60000)
        @Transactional
        public void assignScheduledRides() {

            LocalDateTime now = LocalDateTime.now();

            List<Ride> rides = rideRepository
                    .findByStatusAndScheduledAtBefore(
                            RideStatus.PENDING,
                            now.plusMinutes(10)
                    );

            for (Ride ride : rides) {
                if (ride.getDriver() != null) continue;

                try {

                    Driver driver = driverService.findDriverForRide(ride);

                    ride.setDriver(driver);
                    driver.setCurrentRide(ride);
                    driver.setFree(false);
                    driver.setAvailable(false);
                    ride.setStatus(RideStatus.ACCEPTED);

                    ride.setStatus(RideStatus.ACCEPTED);

                    rideRepository.save(ride);

                    for (Passenger p : ride.getPassengers()) {
                        notificationService.createNotification(
                                p.getId(),
                                "Driver assigned for your scheduled ride!",
                                NotificationType.ACCEPTED_RIDE,
                                ride.getId()
                        );
                    }

                } catch (Exception ignored) {
                }
            }
        }

        public RideReportResponse generateReport(
                LocalDateTime from,
                LocalDateTime to,
                Long userId
        ) {


            List<Ride> rides;

            if (userId != null) {
                rides = rideRepository.findCompletedBetweenForUser(from, to, userId);
            } else {
                rides = rideRepository.findCompletedBetween(from, to);
            }

            if (userId != null) {
                rides = rides.stream()
                        .filter(r ->

                                (r.getDriver() != null &&
                                        r.getDriver().getId().equals(userId))

                                        ||

                                        (r.getCreator() != null &&
                                                r.getCreator().getId().equals(userId))

                                        ||

                                        (r.getPassengers() != null &&
                                                r.getPassengers().stream()
                                                        .anyMatch(p -> p.getId().equals(userId)))
                        )
                        .toList();
            }

            Map<LocalDate, List<Ride>> byDate =
                    rides.stream()
                            .collect(Collectors.groupingBy(
                                    r -> r.getFinishedAt().toLocalDate()
                            ));

            List<DailyReportItem> daily = new ArrayList<>();

            long totalRides = 0;
            double totalDistance = 0;
            double totalMoney = 0;

            for (var entry : byDate.entrySet()) {

                LocalDate date = entry.getKey();
                List<Ride> dayRides = entry.getValue();

                long rideCount = dayRides.size();
                double dist = dayRides.stream().mapToDouble(Ride::getDistance).sum();
                double money = dayRides.stream().mapToDouble(Ride::getCalculatedPrice).sum();

                daily.add(new DailyReportItem(date, rideCount, dist, money));

                totalRides += rideCount;
                totalDistance += dist;
                totalMoney += money;
            }

            long days = Math.max(1, byDate.size());

            return new RideReportResponse(
                    daily,
                    totalRides,
                    totalDistance,
                    totalMoney,
                    (double) totalRides / days,
                    totalDistance / days,
                    totalMoney / days
            );
        }
    }