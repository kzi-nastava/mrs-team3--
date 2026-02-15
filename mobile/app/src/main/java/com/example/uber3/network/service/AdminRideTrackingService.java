package com.example.uber3.network.service;

import android.content.Context;
import androidx.annotation.NonNull;
import com.example.uber3.network.api.AdminRideTrackingApiService;
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.model.admin.AdminRideTrackingData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRideTrackingService {

    public interface RideDataCallback {
        void onSuccess(AdminRideTrackingData data);
        void onError(String message);
    }

    public static void getRideByDriverId(Context context, long driverId,
                                         RideDataCallback callback) {
        AdminRideTrackingApiService apiService = ApiClient.getClient(context)
                .create(AdminRideTrackingApiService.class);

        apiService.getRideByDriverId(driverId).enqueue(new Callback<AdminRideTrackingData>() {
            @Override
            public void onResponse(@NonNull Call<AdminRideTrackingData> call,
                                   @NonNull Response<AdminRideTrackingData> response) {
                if (response.code() == 204 || response.code() == 404) {
                    callback.onError("204");
                } else if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load ride information");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AdminRideTrackingData> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void getRideById(Context context, long rideId, RideDataCallback callback) {
        AdminRideTrackingApiService apiService = ApiClient.getClient(context)
                .create(AdminRideTrackingApiService.class);

        apiService.getRideById(rideId).enqueue(new Callback<AdminRideTrackingData>() {
            @Override
            public void onResponse(@NonNull Call<AdminRideTrackingData> call,
                                   @NonNull Response<AdminRideTrackingData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load ride information");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AdminRideTrackingData> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}