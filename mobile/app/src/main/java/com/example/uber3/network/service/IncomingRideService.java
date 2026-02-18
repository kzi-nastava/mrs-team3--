package com.example.uber3.network.service;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.RideTrackingApiService;
import com.example.uber3.network.model.tracking.IncomingRideResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingRideService {

    private final RideTrackingApiService api;

    public IncomingRideService(Context ctx) {
        api = ApiClient.getClient(ctx).create(RideTrackingApiService.class);
    }

    public interface IncomingRidesCallback {
        void onSuccess(List<IncomingRideResponse> rides);
        void onError(String errorMessage);
    }

    public void getIncomingRides(IncomingRidesCallback cb) {
        api.getAllIncomingRidesForPassenger().enqueue(new Callback<List<IncomingRideResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<IncomingRideResponse>> call,
                                   @NonNull Response<List<IncomingRideResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cb.onSuccess(response.body());
                } else {
                    cb.onError("Failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<IncomingRideResponse>> call,
                                  @NonNull Throwable t) {
                cb.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    public void cancelRide(Long rideId, SimpleCallback cb) {
        api.cancelRideByPassenger(rideId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) cb.onSuccess();
                else cb.onError("Failed: " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                cb.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }
}
