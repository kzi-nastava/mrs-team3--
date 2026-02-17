package com.example.uber3.network.service;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.PricingApi;
import com.example.uber3.network.model.pricing.PricingChangeRequest;
import com.example.uber3.network.model.pricing.PricingConstraints;
import com.example.uber3.network.model.pricing.PricingResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PricingService {

    public interface PricingCallback {
        void onSuccess(PricingResponse response);
        void onError(String message);
    }

    public interface ConstraintsCallback {
        void onSuccess(PricingConstraints constraints);
        void onError(String message);
    }

    public interface UpdateCallback {
        void onSuccess(PricingResponse response);
        void onError(String message);
    }

    public static void getCurrentPricing(Context context, PricingCallback callback) {
        PricingApi api = ApiClient.getClient(context).create(PricingApi.class);

        api.getCurrentPricing().enqueue(new Callback<PricingResponse>() {
            @Override
            public void onResponse(@NonNull Call<PricingResponse> call,
                                   @NonNull Response<PricingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load pricing: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PricingResponse> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void getPricingConstraints(Context context, ConstraintsCallback callback) {
        PricingApi api = ApiClient.getClient(context).create(PricingApi.class);

        api.getPricingConstraints().enqueue(new Callback<PricingConstraints>() {
            @Override
            public void onResponse(@NonNull Call<PricingConstraints> call,
                                   @NonNull Response<PricingConstraints> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load constraints: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PricingConstraints> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void updatePricing(Context context, PricingChangeRequest request,
                                     UpdateCallback callback) {
        PricingApi api = ApiClient.getClient(context).create(PricingApi.class);

        api.updatePricing(request).enqueue(new Callback<PricingResponse>() {
            @Override
            public void onResponse(@NonNull Call<PricingResponse> call,
                                   @NonNull Response<PricingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String errorMsg = "Update failed (" + response.code() + ")";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PricingResponse> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Validation helper
    public static String validatePricing(PricingChangeRequest request,
                                         PricingConstraints constraints,
                                         PricingResponse current) {
        double finalStandard = request.standardBasePrice != null ?
                request.standardBasePrice : current.standardBasePrice;
        double finalLuxury = request.luxuryBasePrice != null ?
                request.luxuryBasePrice : current.luxuryBasePrice;
        double finalVan = request.vanBasePrice != null ?
                request.vanBasePrice : current.vanBasePrice;
        double finalPricePerKm = request.pricePerKm != null ?
                request.pricePerKm : current.pricePerKm;

        // Range validation
        if (request.standardBasePrice != null) {
            if (request.standardBasePrice < constraints.minBasePrice) {
                return "Standard base price must be at least " + constraints.minBasePrice;
            }
            if (request.standardBasePrice > constraints.maxBasePrice) {
                return "Standard base price cannot exceed " + constraints.maxBasePrice;
            }
        }

        if (request.luxuryBasePrice != null) {
            if (request.luxuryBasePrice < constraints.minBasePrice) {
                return "Luxury base price must be at least " + constraints.minBasePrice;
            }
            if (request.luxuryBasePrice > constraints.maxBasePrice) {
                return "Luxury base price cannot exceed " + constraints.maxBasePrice;
            }
        }

        if (request.vanBasePrice != null) {
            if (request.vanBasePrice < constraints.minBasePrice) {
                return "Van base price must be at least " + constraints.minBasePrice;
            }
            if (request.vanBasePrice > constraints.maxBasePrice) {
                return "Van base price cannot exceed " + constraints.maxBasePrice;
            }
        }

        if (request.pricePerKm != null) {
            if (request.pricePerKm < constraints.minPricePerKm) {
                return "Price per km must be at least " + constraints.minPricePerKm;
            }
            if (request.pricePerKm > constraints.maxPricePerKm) {
                return "Price per km cannot exceed " + constraints.maxPricePerKm;
            }
        }

        // Business rules validation
        if (finalLuxury <= finalStandard) {
            return "Luxury price (" + finalLuxury + ") must be higher than standard (" + finalStandard + ")";
        }

        if (finalVan <= finalLuxury) {
            return "Van price (" + finalVan + ") must be higher than luxury (" + finalLuxury + ")";
        }

        return null; // Valid
    }
}