package com.example.uber3.network.api;

import com.example.uber3.network.model.admin.AdminRideTrackingData;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface AdminRideTrackingApiService {

    @GET("api/admin/drivers/{driverId}/current-ride")
    Call<AdminRideTrackingData> getRideByDriverId(@Path("driverId") long driverId);

    @GET("api/admin/rides/{rideId}")
    Call<AdminRideTrackingData> getRideById(@Path("rideId") long rideId);
}