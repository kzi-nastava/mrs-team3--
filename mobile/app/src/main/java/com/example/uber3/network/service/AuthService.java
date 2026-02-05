package com.example.uber3.network.service;

import android.content.Context;

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ApiService;
import com.example.uber3.network.manager.TokenManager;
import com.example.uber3.network.model.LoginRequest;
import com.example.uber3.network.model.LoginResponse;
import com.example.uber3.network.model.auth.RegisterRequest;
import com.example.uber3.network.model.auth.RegisterResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthService {

    public interface LoginCallback {
        void onSuccess(LoginResponse response);
        void onError(String message);
    }

    public interface RegisterCallback {
        void onSuccess(RegisterResponse response);
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

    public static void register(Context context,
                                            String email,
                                            String password,
                                            String name,
                                            String surname,
                                            String phoneNumber,
                                            String address,
                                            String base64Image,
                                            String extension,
                                            RegisterCallback callback){

        if (password == null || password.trim().isEmpty()) {
            callback.onError("Password cannot be empty");
            return;
        }

        if (password.length() < 8) {
            callback.onError("Password must be at least 8 characters");
            return;
        }

        if (phoneNumber == null || !phoneNumber.matches("\\d+")) {
            callback.onError("Phone number must contain only digits");
            return;
        }

        ApiService apiService = ApiClient.getClient(context).create(ApiService.class);

        RegisterRequest request = new RegisterRequest(email, password, name, surname, phoneNumber,address, base64Image, extension);
        apiService.registerPassenger(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String msg = "Registration failed (" + response.code() + ")";
                    try {
                        if (response.errorBody() != null) msg = response.errorBody().string();
                    } catch (Exception ignored) {}
                    callback.onError(msg);
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}
