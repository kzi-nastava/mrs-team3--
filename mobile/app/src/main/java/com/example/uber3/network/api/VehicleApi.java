package com.example.uber3.network.api;

import com.example.uber3.network.model.location.ActiveVehicle;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface VehicleApi {

    @GET("api/vehicles/active")
    Call<List<ActiveVehicle>> getActiveVehicles();
}