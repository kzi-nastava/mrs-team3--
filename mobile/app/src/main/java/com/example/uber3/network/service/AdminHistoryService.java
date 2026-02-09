package com.example.uber3.network.service;

import android.content.Context;
import android.util.Log;

import com.example.uber3.network.api.AdminHistoryApi;
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.model.history.AdminRideHistoryExtendedResponse;
import com.example.uber3.network.model.history.AdminRideHistoryResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminHistoryService {

    private static final String TAG = "AdminHistoryService";

    private final AdminHistoryApi api;

    public AdminHistoryService(Context context) {
        this.api = ApiClient.getClient(context).create(AdminHistoryApi.class);
    }

    public interface RideHistoryCallback {
        void onSuccess(List<AdminRideHistoryResponse> rides);
        void onError(String errorMessage);
    }

    public interface RideDetailCallback {
        void onSuccess(AdminRideHistoryExtendedResponse rideDetail);
        void onError(String errorMessage);
    }

    public void getAdminRideHistory(RideHistoryCallback callback) {
        Log.d(TAG, "Fetching admin ride history...");

        api.getAdminRideHistory().enqueue(new Callback<List<AdminRideHistoryResponse>>() {
            @Override
            public void onResponse(Call<List<AdminRideHistoryResponse>> call,
                                   Response<List<AdminRideHistoryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load admin rides: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<AdminRideHistoryResponse>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getAdminRideDetail(Long rideId, RideDetailCallback callback) {
        if (rideId == null) {
            callback.onError("Ride ID is required");
            return;
        }

        Log.d(TAG, "Fetching admin ride detail: " + rideId);

        api.getAdminRideDetail(rideId).enqueue(new Callback<AdminRideHistoryExtendedResponse>() {
            @Override
            public void onResponse(Call<AdminRideHistoryExtendedResponse> call,
                                   Response<AdminRideHistoryExtendedResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load ride detail: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<AdminRideHistoryExtendedResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}
