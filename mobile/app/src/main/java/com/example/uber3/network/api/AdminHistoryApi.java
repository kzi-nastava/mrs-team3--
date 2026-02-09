package com.example.uber3.network.api;

import com.example.uber3.network.model.history.AdminRideHistoryExtendedResponse;
import com.example.uber3.network.model.history.AdminRideHistoryResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface AdminHistoryApi {

    @GET("api/rides/history/admin")
    Call<List<AdminRideHistoryResponse>> getAdminRideHistory();


    @GET("api/rides/history/admin/{rideId}")
    Call<AdminRideHistoryExtendedResponse> getAdminRideDetail(@Path("rideId") Long rideId);
}
