package com.example.uber3.network.service;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.VehicleApi;
import com.example.uber3.network.model.location.ActiveVehicle;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VehicleService {

    public interface VehiclesCallback {
        void onSuccess(List<ActiveVehicle> vehicles);
        void onError(String message);
    }

    public static void getActiveVehicles(Context context, VehiclesCallback callback) {
        VehicleApi api = ApiClient.getClient(context).create(VehicleApi.class);

        api.getActiveVehicles().enqueue(new Callback<List<ActiveVehicle>>() {
            @Override
            public void onResponse(@NonNull Call<List<ActiveVehicle>> call,
                                   @NonNull Response<List<ActiveVehicle>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load vehicles: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ActiveVehicle>> call,
                                  @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}