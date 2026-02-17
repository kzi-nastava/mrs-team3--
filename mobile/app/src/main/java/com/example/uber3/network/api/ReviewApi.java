package com.example.uber3.network.api;

import com.example.uber3.network.model.review.RideReviewDetailResponse;
import com.example.uber3.network.model.review.RideReviewRequest;
import com.example.uber3.network.model.review.RideReviewResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ReviewApi {

    /**
     * Get ride details for reviewing
     */
    @GET("api/rides/{rideId}/review-details")
    Call<RideReviewDetailResponse> getRideReviewDetails(
            @Header("Authorization") String bearerToken,
            @Path("rideId") Long rideId
    );

    /**
     * Submit or update a review
     */
    @POST("api/rides/{rideId}/review")
    Call<RideReviewResponse> submitReview(
            @Header("Authorization") String bearerToken,
            @Path("rideId") Long rideId,
            @Body RideReviewRequest request
    );
}