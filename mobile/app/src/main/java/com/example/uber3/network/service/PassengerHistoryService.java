package com.example.uber3.network.service;

import android.content.Context;

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.PassengerHistoryApi;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.history.PassengerRideSummaryExtendedResponse;
import com.example.uber3.network.model.history.PassengerRideSummaryResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PassengerHistoryService {

    public interface RideHistoryCallback {
        void onSuccess(List<PassengerRideSummaryResponse> rides);
        void onError(String errorMessage);
    }

    public interface RideDetailCallback {
        void onSuccess(PassengerRideSummaryExtendedResponse ride);
        void onError(String errorMessage);
    }

    private final PassengerHistoryApi api;
    private final Context context;

    public PassengerHistoryService(Context context) {
        this.context = context.getApplicationContext();
        this.api = ApiClient.getClient(context)
                .create(PassengerHistoryApi.class);
    }

    // ================= LIST =================
    public void getPassengerRideHistory(RideHistoryCallback callback) {
        String token = getToken();
        if (token == null) {
            callback.onError("Unauthorized (missing token)");
            return;
        }

        api.getPassengerRideHistory("Bearer " + token)
                .enqueue(new Callback<List<PassengerRideSummaryResponse>>() {
                    @Override
                    public void onResponse(Call<List<PassengerRideSummaryResponse>> call,
                                           Response<List<PassengerRideSummaryResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError(parseError(response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<List<PassengerRideSummaryResponse>> call,
                                          Throwable t) {
                        callback.onError(t.getMessage() != null
                                ? t.getMessage()
                                : "Network error");
                    }
                });
    }

    // ================= DETAIL =================
    public void getPassengerRideDetail(Long rideId, RideDetailCallback callback) {
        String token = getToken();
        if (token == null) {
            callback.onError("Unauthorized (missing token)");
            return;
        }

        api.getPassengerRideDetail("Bearer " + token, rideId)
                .enqueue(new Callback<PassengerRideSummaryExtendedResponse>() {
                    @Override
                    public void onResponse(Call<PassengerRideSummaryExtendedResponse> call,
                                           Response<PassengerRideSummaryExtendedResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError(parseError(response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<PassengerRideSummaryExtendedResponse> call,
                                          Throwable t) {
                        callback.onError(t.getMessage() != null
                                ? t.getMessage()
                                : "Network error");
                    }
                });
    }

    // ================= HELPERS =================
    private String parseError(int code) {
        switch (code) {
            case 401: return "Unauthorized (401)";
            case 403: return "Forbidden (403)";
            case 404: return "Ride not found";
            default:  return "Failed to load passenger rides (" + code + ")";
        }
    }

    private String getToken() {
        return TokenManager.getToken(context);
    }

}
