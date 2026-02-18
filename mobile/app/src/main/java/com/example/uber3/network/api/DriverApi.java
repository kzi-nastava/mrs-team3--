package com.example.uber3.network.api;

import com.example.uber3.network.model.driver.DriverStatusResponse;
import com.example.uber3.network.model.history.DriverRideHistoryDetailResponse;
import com.example.uber3.network.model.history.DriverRideHistoryResponse;
import com.example.uber3.network.model.register.RegisterDriverRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DriverApi {

    @POST("api/drivers")
    Call<Void> registerDriver(@Body RegisterDriverRequest request);

    @GET("api/drivers/{id}/rides")
    Call<List<DriverRideHistoryResponse>> getDriverRideHistory(
            @Path("id") Long driverId,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );


    @GET("api/drivers/{driverId}/rides/{rideId}")
    Call<DriverRideHistoryDetailResponse> getDriverRideDetail(
            @Path("driverId") Long driverId,
            @Path("rideId") Long rideId
    );

    @PUT("api/drivers/change-active-status")
    Call<DriverStatusResponse> toggleDriverActiveStatus();

    @GET("api/drivers/status")
    Call<DriverStatusResponse> getDriverStatus();

}
