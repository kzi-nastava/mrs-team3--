package com.example.uber3.network.api;

import com.example.uber3.network.model.register.RegisterDriverRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface DriverApi {

    @POST("api/drivers")
    Call<Void> registerDriver(@Body RegisterDriverRequest request);

}
