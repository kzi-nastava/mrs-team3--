package com.example.uber3.network.service;

import android.content.Context;
import androidx.annotation.NonNull;
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.RideTrackingApiService;
import com.example.uber3.network.model.tracking.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideTrackingService {

    public interface RideDataCallback {
        void onSuccess(RideTrackingData data);
        void onError(String message);
    }

    public interface ValidateTokenCallback {
        void onSuccess(TokenValidation validation);
        void onError(String message);
    }

    public interface ReportCallback {
        void onSuccess(ReportResponse response);
        void onError(String message);
    }

    public interface PanicCallback {
        void onSuccess();
        void onError(String message);
    }

    public static void getCurrentRide(Context context, RideDataCallback callback) {
        RideTrackingApiService apiService = ApiClient.getClient(context)
                .create(RideTrackingApiService.class);

        apiService.getCurrentRide().enqueue(new Callback<RideTrackingData>() {
            @Override
            public void onResponse(@NonNull Call<RideTrackingData> call,
                                   @NonNull Response<RideTrackingData> response) {
                if (response.code() == 204) {
                    callback.onError("204");
                } else if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load ride information");
                }
            }

            @Override
            public void onFailure(@NonNull Call<RideTrackingData> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void validateToken(Context context, String token,
                                     ValidateTokenCallback callback) {
        RideTrackingApiService apiService = ApiClient.getClient(context)
                .create(RideTrackingApiService.class);

        apiService.validateToken(token).enqueue(new Callback<TokenValidation>() {
            @Override
            public void onResponse(@NonNull Call<TokenValidation> call,
                                   @NonNull Response<TokenValidation> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Invalid token");
                }
            }

            @Override
            public void onFailure(@NonNull Call<TokenValidation> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void getRideByToken(Context context, String token,
                                      RideDataCallback callback) {
        RideTrackingApiService apiService = ApiClient.getClient(context)
                .create(RideTrackingApiService.class);

        apiService.getRideByToken(token).enqueue(new Callback<RideTrackingData>() {
            @Override
            public void onResponse(@NonNull Call<RideTrackingData> call,
                                   @NonNull Response<RideTrackingData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load ride information");
                }
            }

            @Override
            public void onFailure(@NonNull Call<RideTrackingData> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void reportInconsistencyForCurrentRide(Context context, String reportText,
                                                         ReportCallback callback) {
        RideTrackingApiService apiService = ApiClient.getClient(context)
                .create(RideTrackingApiService.class);

        ReportRequest request = new ReportRequest(reportText);

        apiService.reportInconsistency(request).enqueue(new Callback<ReportResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReportResponse> call,
                                   @NonNull Response<ReportResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to submit report");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReportResponse> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void reportInconsistencyByToken(Context context, String token,
                                                  String reportText, ReportCallback callback) {
        RideTrackingApiService apiService = ApiClient.getClient(context)
                .create(RideTrackingApiService.class);

        ReportRequest request = new ReportRequest(reportText);

        apiService.reportInconsistencyByToken(token, request)
                .enqueue(new Callback<ReportResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ReportResponse> call,
                                           @NonNull Response<ReportResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("Failed to submit report");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ReportResponse> call, @NonNull Throwable t) {
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
    }

    public static void panic(Context context, long rideId, PanicCallback callback) {
        RideTrackingApiService apiService = ApiClient.getClient(context)
                .create(RideTrackingApiService.class);

        apiService.panic(rideId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Failed to send panic alert");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void panicByToken(Context context, String token, PanicCallback callback) {
        RideTrackingApiService apiService = ApiClient.getClient(context)
                .create(RideTrackingApiService.class);

        apiService.panicByToken(token).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Failed to send panic alert");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}