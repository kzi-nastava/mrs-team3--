package com.example.uber3.network.api;

import com.example.uber3.network.model.history.PassengerRideSummaryResponse;
import com.example.uber3.network.model.history.PassengerRideSummaryExtendedResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface PassengerHistoryApi {

    @GET("api/rides/history/passenger")
    Call<List<PassengerRideSummaryResponse>> getPassengerRideHistory(
            @Header("Authorization") String bearerToken
    );

    @GET("api/rides/history/passenger/{rideId}")
    Call<PassengerRideSummaryExtendedResponse> getPassengerRideDetail(
            @Header("Authorization") String bearerToken,
            @Path("rideId") Long rideId
    );
}
