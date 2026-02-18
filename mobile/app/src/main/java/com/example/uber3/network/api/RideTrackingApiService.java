package com.example.uber3.network.api;

import com.example.uber3.network.model.tracking.*;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.*;

public interface RideTrackingApiService {

    @GET("api/ride-tracking/current")
    Call<RideTrackingData> getCurrentRide();

    @GET("api/ride-tracking/validate/{token}")
    Call<TokenValidation> validateToken(@Path("token") String token);

    @GET("api/ride-tracking/token/{token}")
    Call<RideTrackingData> getRideByToken(@Path("token") String token);

    @POST("api/ride-tracking/current/report")
    Call<ReportResponse> reportInconsistency(@Body ReportRequest request);

    @POST("api/ride-tracking/token/{token}/report")
    Call<ReportResponse> reportInconsistencyByToken(
            @Path("token") String token,
            @Body ReportRequest request
    );

    @POST("api/ride-tracking/{rideId}/panic")
    Call<Void> panic(@Path("rideId") long rideId);

    @POST("api/ride-tracking/token/{token}/panic")
    Call<Void> panicByToken(@Path("token") String token);

    @GET("api/rides/incoming-rides/passenger")
    Call<List<IncomingRideResponse>> getAllIncomingRidesForPassenger();

    @POST("api/rides/incoming-rides/passenger/{rideId}/cancel")
    Call<Void> cancelRideByPassenger(@Path("rideId") Long rideId);
}