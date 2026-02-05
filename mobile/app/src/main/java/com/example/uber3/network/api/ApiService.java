package com.example.uber3.network.api;

import com.example.uber3.network.model.DriverProfileChangeRequestDto;
import com.example.uber3.network.model.ProfileResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Body;
import com.example.uber3.network.model.UpdateProfileRequest;
import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import com.example.uber3.network.model.LoginRequest;
import com.example.uber3.network.model.LoginResponse;
import com.example.uber3.network.model.auth.RegisterRequest;
import com.example.uber3.network.model.auth.RegisterResponse;
import com.example.uber3.network.model.mails.ForgotPasswordRequest;
import com.example.uber3.network.model.mails.ResetPasswordRequest;

public interface ApiService {

    @GET("api/profile/me")
    Call<ProfileResponse> getMyProfile();

    @PUT("api/profile/me")
    Call<Void> updateProfile(@Body UpdateProfileRequest request);

    @Multipart
    @POST("api/profile/me/image")
    Call<String> uploadProfileImage(
            @Part MultipartBody.Part file
    );

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/api/profile/change-request")
    Call<Void> submitDriverChangeRequest(
            @Body DriverProfileChangeRequestDto dto
    );


    @POST("api/auth/forgot-password")
    Call<Void> forgotPassword(
            @Body ForgotPasswordRequest request
    );

    @POST("api/auth/reset-password")
    Call<Void> resetPassword(
            @Body ResetPasswordRequest request
    );

    @POST("api/auth/register")
    Call<RegisterResponse> registerPassenger(@Body RegisterRequest request);
}
