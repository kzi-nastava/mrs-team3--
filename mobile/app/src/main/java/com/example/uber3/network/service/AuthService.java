package com.example.uber3.network.service;

import android.content.Context;

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ApiService;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.LoginRequest;
import com.example.uber3.network.model.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthService {

    public interface LoginCallback {
        void onSuccess(LoginResponse response);
        void onError(String message);
    }

    public static void login(Context context,
                             String email,
                             String password,
                             LoginCallback callback) {

        ApiService apiService = ApiClient
                .getClient(context)
                .create(ApiService.class);

        LoginRequest request = new LoginRequest(email, password);

        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call,
                                   Response<LoginResponse> response) {

                if (response.isSuccessful() && response.body() != null) {

                    String token = response.body().token;
                    TokenManager.saveToken(context, token);
                    TokenManager.saveRole(context, response.body().role);
                    TokenManager.saveUserEmail(context, response.body().email);


                    callback.onSuccess(response.body());

                } else {
                    callback.onError("Invalid credentials");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}
