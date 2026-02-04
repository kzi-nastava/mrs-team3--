package com.example.uber3.network.service;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ORSService {

    @POST("v2/directions/driving-car/geojson")
    Call<String> getRoute(
            @Header("Authorization") String apiKey,
            @Body RequestBody body
    );
}
