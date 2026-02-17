package com.example.uber3.network.api;

import com.example.uber3.network.model.pricing.PricingChangeRequest;
import com.example.uber3.network.model.pricing.PricingConstraints;
import com.example.uber3.network.model.pricing.PricingResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;

public interface PricingApi {
    @GET("api/pricing")
    Call<PricingResponse> getCurrentPricing();

    @PUT("api/pricing")
    Call<PricingResponse> updatePricing(@Body PricingChangeRequest request);

    @GET("api/pricing/constraints")
    Call<PricingConstraints> getPricingConstraints();
}
