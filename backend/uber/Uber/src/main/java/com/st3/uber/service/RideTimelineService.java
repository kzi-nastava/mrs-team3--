package com.st3.uber.service;

import com.st3.uber.domain.*;
import com.st3.uber.dto.ride.IncomingRideResponse;
import com.st3.uber.dto.ride.InconsistencyReportItemResponse;
import com.st3.uber.dto.rideHistory.AdminRideHistoryExtendedResponse;
import com.st3.uber.dto.rideHistory.AdminRideHistoryResponse;
import com.st3.uber.dto.rideHistory.PassengerRideSummaryExtendedResponse;
import com.st3.uber.dto.rideHistory.PassengerRideSummaryResponse;
import com.st3.uber.enums.RideStatus;
import com.st3.uber.repository.PassengerRepository;
import com.st3.uber.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RideTimelineService {
  private final RideRepository rideRepository;
  private final PassengerRepository passengerRepository;

  public RideTimelineService(RideRepository rideRepository, PassengerRepository passengerRepository) {
    this.rideRepository = rideRepository;
    this.passengerRepository = passengerRepository;
  }

  @Transactional
  public List<AdminRideHistoryResponse> adminRideHistory() {
    List<RideStatus> statuses = List.of(RideStatus.COMPLETED, RideStatus.CANCELLED_BY_DRIVER,
        RideStatus.CANCELLED_BY_PASSENGER, RideStatus.FINISHED_EARLY);

    List<Ride> rides = rideRepository.getAllByStatusIn(statuses);
    return rides.stream().map(r -> {

      AdminRideHistoryResponse res = new AdminRideHistoryResponse();
      fillCommonRideFields(r, res);
      return res;
    }).toList();
  }

  @Transactional
  public AdminRideHistoryExtendedResponse adminRideHistoryDetails(Long rideId) {
    Ride ride = rideRepository.findById(rideId)
        .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

    validateRideForHistory(ride);

    AdminRideHistoryExtendedResponse res = new AdminRideHistoryExtendedResponse();

    fillCommonRideFields(ride, res);

    fillExtendedFields(ride, res);

    return res;
  }

  private void fillCommonRideFields(Ride ride, AdminRideHistoryResponse res) {
    res.setId(ride.getId());
    res.setPrice(ride.getCalculatedPrice());
    res.setPanic(ride.isPanic());
    res.setStatus(ride.getStatus());
    res.setStartLocation(ride.getStartLocation());

    if (ride.getActualEndLocation() == null) {
      res.setEndLocation(ride.getEndLocation());
    } else {
      res.setEndLocation(ride.getActualEndLocation());
    }

    if (ride.getStartedAt() != null)
      res.setStartTime(ride.getStartedAt());
    else if(ride.getScheduledAt() != null)
      res.setStartTime(ride.getScheduledAt());
    else
      res.setStartTime(ride.getCreatedAt());

    res.setEndTime(ride.getFinishedAt());
  }

  private void fillExtendedFields(Ride ride, AdminRideHistoryExtendedResponse res) {

    if (ride.getDriver() == null) {
      res.setDriverName("-");
    } else {
      Driver driver = ride.getDriver();
      String name = driver.getName() == null ? "" : driver.getName();
      String surname = driver.getSurname() == null ? "" : driver.getSurname();
      res.setDriverName((name + surname).isBlank() ? "-" : name + " " + surname);
    }

    List<String> passengerEmails = ride.getPassengers() == null ? List.of() : ride.getPassengers().stream()
        .map(Passenger::getEmail)
        .toList();

    List<String> userEmails = ride.getInvites() == null ? List.of() : ride.getInvites().stream()
        .map(RideInvite::getEmail)
        .toList();

    res.setPassengerEmails(Stream.concat(passengerEmails.stream(), userEmails.stream()).toList());

    res.setStops(
        ride.getRideStops() == null ? List.of() : ride.getRideStops()
    );

    List<Review> reviews = ride.getReviews();
    if (reviews == null || reviews.isEmpty()) {
      res.setDriverReview(null);
      res.setRideReview(null);
    } else {
      res.setDriverReview(
          calculateReview(reviews.stream()
              .map(Review::getDriverRating)
              .toList())
      );
      res.setRideReview(
          calculateReview(reviews.stream()
              .map(Review::getVehicleRating)
              .toList())
      );
    }

    res.setInconsistencyReports(
        ride.getInconsistencyReports() == null
            ? List.of()
            : ride.getInconsistencyReports().stream()
            .map(r -> {
              InconsistencyReportItemResponse dto = new InconsistencyReportItemResponse();
              dto.setId(r.getId());
              dto.setReportText(r.getReportText());
              dto.setCreatedAt(r.getCreatedAt());
              return dto;
            })
            .toList()
    );

    res.setCancellationReason(ride.getTerminationReason());

  }
  private void validateRideForHistory(Ride ride) {
    if (ride.getStatus() != RideStatus.COMPLETED &&
        ride.getStatus() != RideStatus.CANCELLED_BY_DRIVER &&
        ride.getStatus() != RideStatus.CANCELLED_BY_PASSENGER &&
        ride.getStatus() != RideStatus.FINISHED_EARLY) {

      throw new IllegalStateException(
          "Can only view details for completed or cancelled rides"
      );
    }
  }

  public PassengerRideSummaryExtendedResponse getPastRideDetails(Long id, Long rideId) {
    Passenger passenger = passengerRepository.findById(id).orElseThrow(
        () -> new IllegalArgumentException("Passenger not found."));
    Ride ride = rideRepository.findById(rideId).orElseThrow(
        () -> new IllegalArgumentException("Ride not found.")
    );

    if (!ride.getCreator().equals(passenger)) {
      throw new IllegalArgumentException("Passenger did not create this ride.");
    }
    boolean favorite = passenger.getFavoriteRides() != null &&
        passenger.getFavoriteRides().stream().anyMatch(r -> r.getId().equals(ride.getId()));

    return detailsResponse(ride, favorite);
  }

  public List<IncomingRideResponse> getAllIncomingRidesForPassenger(Long id) {
    Passenger passenger = passengerRepository.findById(id).orElseThrow(
        () -> new IllegalArgumentException("Passenger not found."));

    List<Ride> rides = rideRepository.findByCreatorAndScheduledAtAfterAndStatusIn(
        passenger,
        LocalDateTime.now(),
        List.of(RideStatus.ACCEPTED, RideStatus.PENDING)
    );

    return rides.stream().map(r -> {
      IncomingRideResponse item = new IncomingRideResponse();
      item.setId(r.getId());
      item.setStartLocation(r.getStartLocation());
      item.setEndLocation(r.getEndLocation());
      item.setStartTime(r.getScheduledAt());
      return item;
    }).toList();

  }

  public List<PassengerRideSummaryResponse> getPastPassengerRides(Long id) {
    Passenger passenger = passengerRepository.findById(id).orElseThrow(
        () -> new IllegalArgumentException("Passenger not found."));

    List<Ride> rides = rideRepository.findByCreatorAndFinishedAtIsNotNull(passenger);

    List<Ride> favoriteRides = passenger.getFavoriteRides();
    Set<Long> favoriteRideIds = favoriteRides.stream()
        .map(Ride::getId)
        .collect(Collectors.toSet());

    return rides.stream()
        .map(r -> new PassengerRideSummaryResponse(
            r.getId(),
            r.getStatus(),
            r.getStartLocation(),
            r.getActualEndLocation(),
            r.getStartedAt(),
            r.getFinishedAt(),
            favoriteRideIds.contains(r.getId())
        ))
        .toList();
  }

  private static PassengerRideSummaryExtendedResponse detailsResponse(Ride ride, boolean favorite) {
    PassengerRideSummaryExtendedResponse res = new PassengerRideSummaryExtendedResponse();

    res.setId(ride.getId());
    res.setStatus(ride.getStatus());
    res.setStartLocation(ride.getStartLocation());
    res.setEndLocation(ride.getActualEndLocation());
    res.setStartTime(ride.getStartedAt());
    res.setEndTime(ride.getFinishedAt());
    res.setFavorite(favorite);

    res.setStops(ride.getRideStops() == null ? List.of() : ride.getRideStops());

    if (ride.getDriver() == null){
      res.setDriverName("-");
    }
    else{
      Driver driver = ride.getDriver();
      String name = driver.getName() == null ? "" : driver.getName();
      String surname = driver.getSurname() == null ? "" : driver.getSurname();
      res.setDriverName((name + surname).isBlank() ? "-" : name + surname);
    }

    List<Review> reviews = ride.getReviews();
    if (reviews == null || reviews.isEmpty()) {
      res.setDriverReview(null);
      res.setRideReview(null);
    } else {
      res.setDriverReview(calculateReview(reviews.stream().map(Review::getDriverRating).toList()));
      res.setRideReview(calculateReview(reviews.stream().map(Review::getVehicleRating).toList()));
    }
    List<InconsistencyReport> inconsistencyReports = ride.getInconsistencyReports();
    res.setInconsistencyReports(
        inconsistencyReports == null ? List.of() : inconsistencyReports.stream().map(r -> {
          InconsistencyReportItemResponse dto = new InconsistencyReportItemResponse();
          dto.setId(r.getId());
          dto.setReportText(r.getReportText());
          dto.setCreatedAt(r.getCreatedAt());
          return dto;
        }).toList()
    );

    return res;
  }

  private static Double calculateReview(List<Integer> values){
    if (values == null || values.isEmpty())
      return null;
    long count = values.stream().filter(Objects::nonNull).count();
    if (count == 0)
      return null;
    int sum = values.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
    return sum / (double) count;
  }
}
