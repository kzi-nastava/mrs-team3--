package com.example.uber3.network.api;

import com.example.uber3.network.model.driver.CancelRideRequest;
import com.example.uber3.network.model.driver.DriverRide;
import com.example.uber3.network.model.driver.FinishRideRequest;
import com.example.uber3.network.model.driver.FinishRideResponse;
import com.example.uber3.network.model.driver.PendingRide;
import com.example.uber3.network.model.driver.ReachStopRequest;
import com.example.uber3.network.model.driver.UpdateLocationRequest;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface DriverApiService {

    @GET("api/drivers/all-rides")
    Call<List<DriverRide>> getMyRides();

    @GET("api/drivers/pending-rides")
    Call<List<PendingRide>> getPendingRides();

    @POST("api/drivers/rides/{rideId}/accept")
    Call<DriverRide> acceptRide(@Path("rideId") long rideId);

    @POST("api/drivers/rides/{rideId}/start")
    Call<DriverRide> startRide(@Path("rideId") long rideId);

    @POST("api/drivers/rides/{rideId}/finish")
    Call<FinishRideResponse> finishRide(
            @Path("rideId") long rideId,
            @Body FinishRideRequest request
    );

    @PUT("api/drivers/location")
    Call<Void> updateLocation(@Body UpdateLocationRequest request);

    // Try with empty body object
    @POST("api/drivers/move-to-start")
    Call<Void> moveToStart(@Body Map<String, Object> emptyBody);

    @POST("api/drivers/reach-stop")
    Call<DriverRide> reachStop(@Body ReachStopRequest request);

    @POST("api/drivers/{rideId}/cancel")
    Call<Void> cancelRide(
            @Path("rideId") long rideId,
            @Body CancelRideRequest request
    );

    @POST("api/drivers/rides/{rideId}/panic")
    Call<Void> panicRide(@Path("rideId") long rideId);
}