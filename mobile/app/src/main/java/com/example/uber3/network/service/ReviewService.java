package com.example.uber3.network.service;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ReviewApi;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.review.RideReviewDetailResponse;
import com.example.uber3.network.model.review.RideReviewRequest;
import com.example.uber3.network.model.review.RideReviewResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewService {

    public interface ReviewDetailCallback {
        void onSuccess(RideReviewDetailResponse details);
        void onError(String errorMessage);
    }

    public interface SubmitReviewCallback {
        void onSuccess(RideReviewResponse response);
        void onError(String errorMessage);
    }

    private final ReviewApi api;
    private final Context context;

    public ReviewService(Context context) {
        this.context = context.getApplicationContext();
        this.api = ApiClient.getClient(context).create(ReviewApi.class);
    }

    /**
     * Get ride details for review dialog
     */
    public void getRideReviewDetails(Long rideId, ReviewDetailCallback callback) {
        String token = getToken();
        if (token == null) {
            callback.onError("Unauthorized - please login");
            return;
        }

        api.getRideReviewDetails("Bearer " + token, rideId)
                .enqueue(new Callback<RideReviewDetailResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RideReviewDetailResponse> call,
                                           @NonNull Response<RideReviewDetailResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError(parseError(response.code()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RideReviewDetailResponse> call,
                                          @NonNull Throwable t) {
                        callback.onError(t.getMessage() != null
                                ? t.getMessage()
                                : "Network error");
                    }
                });
    }

    /**
     * Submit or update a review
     */
    public void submitReview(Long rideId, int driverRating, int vehicleRating,
                             String comment, SubmitReviewCallback callback) {
        String token = getToken();
        if (token == null) {
            callback.onError("Unauthorized - please login");
            return;
        }

        // Validate ratings
        if (driverRating < 1 || driverRating > 5) {
            callback.onError("Driver rating must be between 1 and 5");
            return;
        }
        if (vehicleRating < 1 || vehicleRating > 5) {
            callback.onError("Vehicle rating must be between 1 and 5");
            return;
        }

        RideReviewRequest request = new RideReviewRequest(driverRating, vehicleRating, comment);

        api.submitReview("Bearer " + token, rideId, request)
                .enqueue(new Callback<RideReviewResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RideReviewResponse> call,
                                           @NonNull Response<RideReviewResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError(parseError(response.code()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RideReviewResponse> call,
                                          @NonNull Throwable t) {
                        callback.onError(t.getMessage() != null
                                ? t.getMessage()
                                : "Network error");
                    }
                });
    }

    /**
     * Helper to check if ride can be reviewed (within 3 days of completion)
     */
    public static boolean canReviewRide(String endTime) {
        if (endTime == null || endTime.isEmpty()) {
            return false;
        }

        try {
            long endMillis = parseIsoToMillis(endTime);
            long now = System.currentTimeMillis();
            long diff = now - endMillis;

            // 3 days in milliseconds
            long threeDays = 3L * 24 * 60 * 60 * 1000;

            return diff >= 0 && diff <= threeDays;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get days remaining to review
     */
    public static int getDaysRemaining(String reviewDeadline) {
        try {
            long deadlineMillis = parseIsoToMillis(reviewDeadline);
            long now = System.currentTimeMillis();
            long diff = deadlineMillis - now;

            if (diff < 0) return 0;

            return (int) Math.ceil(diff / (24.0 * 60 * 60 * 1000));
        } catch (Exception e) {
            return 0;
        }
    }

    // ================= HELPERS =================

    private String parseError(int code) {
        switch (code) {
            case 400: return "Invalid review data";
            case 401: return "Unauthorized - please login";
            case 403: return "You don't have permission to review this ride";
            case 404: return "Ride not found";
            case 409: return "Review period has expired (3 days limit)";
            default:  return "Failed to process review (" + code + ")";
        }
    }

    private String getToken() {
        return TokenManager.getToken(context);
    }

    private static long parseIsoToMillis(String iso) {
        // Simple ISO date parsing - you can use a library if needed
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(iso.substring(0, Math.min(19, iso.length())));
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}