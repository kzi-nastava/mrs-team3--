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
import retrofit2.http.Query;

import com.example.uber3.network.model.LoginRequest;
import com.example.uber3.network.model.LoginResponse;
import com.example.uber3.network.model.auth.RegisterRequest;
import com.example.uber3.network.model.auth.RegisterResponse;
import com.example.uber3.network.model.mails.ForgotPasswordRequest;
import com.example.uber3.network.model.mails.ResetPasswordRequest;
import com.example.uber3.network.model.ride.CreateRideRequest;
import com.example.uber3.network.model.ride.RideResponse;
import com.example.uber3.network.model.ride.RouteEstimateRequest;
import com.example.uber3.network.model.ride.RouteEstimateResponse;
import com.example.uber3.network.model.report.RideReportResponse;
import com.example.uber3.network.model.user.UserDto;

import java.util.List;

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
    @POST("api/rides/estimate-route")
    Call<RouteEstimateResponse> estimateRoute(
            @Body RouteEstimateRequest request
    );


    @POST("api/rides")
    Call<RideResponse> createRide(@Body CreateRideRequest request);

    @GET("api/rides/reports")
    Call<RideReportResponse> getReport(
            @Query("from") String from,
            @Query("to") String to,
            @Query("userId") Long userId
    );

    @GET("api/admin/users")
    Call<List<UserDto>> getAllUsers();

    @GET("api/auth/verify")
    Call<Void> verifyEmail(@Query("token") String token);





}
