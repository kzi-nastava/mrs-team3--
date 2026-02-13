package com.example.uber3.network.service;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.DriverApiService;
import com.example.uber3.network.model.driver.CancelRideRequest;
import com.example.uber3.network.model.driver.DriverRide;
import com.example.uber3.network.model.driver.FinishRideRequest;
import com.example.uber3.network.model.driver.FinishRideResponse;
import com.example.uber3.network.model.driver.Location;
import com.example.uber3.network.model.driver.PendingRide;
import com.example.uber3.network.model.driver.ReachStopRequest;
import com.example.uber3.network.model.driver.UpdateLocationRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverRideService {

    public interface MyRidesCallback {
        void onSuccess(List<DriverRide> rides);
        void onError(String message);
    }

    public interface PendingRidesCallback {
        void onSuccess(List<PendingRide> rides);
        void onError(String message);
    }

    public interface AcceptRideCallback {
        void onSuccess(DriverRide ride);
        void onError(String message);
    }

    public interface StartRideCallback {
        void onSuccess(DriverRide ride);
        void onError(String message);
    }

    public interface FinishRideCallback {
        void onSuccess(FinishRideResponse response);
        void onError(String message);
    }

    public interface MoveToStartCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface ReachStopCallback {
        void onSuccess(DriverRide ride);
        void onError(String message);
    }

    public interface UpdateLocationCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface CancelRideCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface PanicCallback {
        void onSuccess();
        void onError(String message);
    }

    public static void getMyRides(Context context, MyRidesCallback callback) {
        DriverApiService apiService = ApiClient.getClient(context).create(DriverApiService.class);

        apiService.getMyRides().enqueue(new Callback<List<DriverRide>>() {
            @Override
            public void onResponse(@NonNull Call<List<DriverRide>> call,
                                   @NonNull Response<List<DriverRide>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load rides");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<DriverRide>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void getPendingRides(Context context, PendingRidesCallback callback) {
        DriverApiService apiService = ApiClient.getClient(context).create(DriverApiService.class);

        apiService.getPendingRides().enqueue(new Callback<List<PendingRide>>() {
            @Override
            public void onResponse(@NonNull Call<List<PendingRide>> call,
                                   @NonNull Response<List<PendingRide>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load pending rides");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PendingRide>> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void acceptRide(Context context, long rideId, AcceptRideCallback callback) {
        DriverApiService apiService = ApiClient.getClient(context).create(DriverApiService.class);

        apiService.acceptRide(rideId).enqueue(new Callback<DriverRide>() {
            @Override
            public void onResponse(@NonNull Call<DriverRide> call,
                                   @NonNull Response<DriverRide> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String errorMsg = "Failed to accept ride";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<DriverRide> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void startRide(Context context, long rideId, StartRideCallback callback) {
        DriverApiService apiService = ApiClient.getClient(context).create(DriverApiService.class);

        apiService.startRide(rideId).enqueue(new Callback<DriverRide>() {
            @Override
            public void onResponse(@NonNull Call<DriverRide> call,
                                   @NonNull Response<DriverRide> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String errorMsg = "Failed to start ride";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                        android.util.Log.e("DriverRideService", "startRide error: " + response.code() + " - " + errorMsg);
                    } catch (Exception e) {
                        android.util.Log.e("DriverRideService", "startRide error: " + response.code());
                    }
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<DriverRide> call, @NonNull Throwable t) {
                android.util.Log.e("DriverRideService", "startRide network error: " + t.getMessage(), t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void finishRide(Context context, long rideId, Location actualEndLocation,
                                  FinishRideCallback callback) {
        DriverApiService apiService = ApiClient.getClient(context).create(DriverApiService.class);

        FinishRideRequest request = new FinishRideRequest(actualEndLocation);

        apiService.finishRide(rideId, request).enqueue(new Callback<FinishRideResponse>() {
            @Override
            public void onResponse(@NonNull Call<FinishRideResponse> call,
                                   @NonNull Response<FinishRideResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String errorMsg = "Failed to finish ride";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<FinishRideResponse> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void moveToStart(Context context, MoveToStartCallback callback) {
        DriverApiService apiService = ApiClient.getClient(context).create(DriverApiService.class);

        // Send empty body object like Angular does
        java.util.Map<String, Object> emptyBody = new java.util.HashMap<>();

        apiService.moveToStart(emptyBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    String errorMsg = "Failed to move to start";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                        android.util.Log.e("DriverRideService", "moveToStart error: " + response.code() + " - " + errorMsg);
                    } catch (Exception e) {
                        android.util.Log.e("DriverRideService", "moveToStart error: " + response.code());
                    }
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                android.util.Log.e("DriverRideService", "moveToStart network error: " + t.getMessage(), t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void reachStop(Context context, int stopIndex, ReachStopCallback callback) {
        DriverApiService apiService = ApiClient.getClient(context).create(DriverApiService.class);

        ReachStopRequest request = new ReachStopRequest(stopIndex);

        apiService.reachStop(request).enqueue(new Callback<DriverRide>() {
            @Override
            public void onResponse(@NonNull Call<DriverRide> call,
                                   @NonNull Response<DriverRide> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to mark stop as reached");
                }
            }

            @Override
            public void onFailure(@NonNull Call<DriverRide> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void updateLocation(Context context, double latitude, double longitude,
                                      UpdateLocationCallback callback) {
        DriverApiService apiService = ApiClient.getClient(context).create(DriverApiService.class);

        UpdateLocationRequest request = new UpdateLocationRequest(latitude, longitude);

        apiService.updateLocation(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Failed to update location");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void cancelRide(Context context, long rideId, CancelRideRequest request,
                                  CancelRideCallback callback) {
        DriverApiService apiService = ApiClient.getClient(context).create(DriverApiService.class);

        apiService.cancelRide(rideId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    String errorMsg = "Failed to cancel ride";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void panicRide(Context context, long rideId, PanicCallback callback) {
        DriverApiService apiService = ApiClient.getClient(context).create(DriverApiService.class);

        apiService.panicRide(rideId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    String errorMsg = "Failed to activate panic";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}