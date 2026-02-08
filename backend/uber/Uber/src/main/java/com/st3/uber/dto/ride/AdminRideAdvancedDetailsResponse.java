package com.st3.uber.dto.ride;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class AdminRideAdvancedDetailsResponse {

  @Data
  @NoArgsConstructor
  public static class UserDetails {
    Long id;
    String firstName;
    String lastName;
    String email;

    public UserDetails(Long id, String firstName, String lastName, String email) {
      this.id = id;
      this.firstName = firstName;
      this.lastName = lastName;
      this.email = email;
    }
  }

  @Data
  @NoArgsConstructor
  public static class RideAdvancedDetails{
    List<ReportInconsistencyResponse> issuesReported;
    List<Integer> driverRatings;
    List<Integer> vehicleRatings;
    public RideAdvancedDetails(List<ReportInconsistencyResponse> issuesReported, List<Integer> driverRatings,
                               List<Integer> vehicleRatings) {
      this.issuesReported = issuesReported;
      this.driverRatings = driverRatings;
      this.vehicleRatings = vehicleRatings;
    }
  }

  UserDetails driver;
  List<UserDetails> passengers;
  AdminRideDetailsResponse baseDetails;

  public AdminRideAdvancedDetailsResponse(UserDetails driver, List<UserDetails> passengers,
                                      AdminRideDetailsResponse baseDetails) {
    this.driver = driver;
    this.passengers = passengers;
    this.baseDetails = baseDetails;

  }
}
