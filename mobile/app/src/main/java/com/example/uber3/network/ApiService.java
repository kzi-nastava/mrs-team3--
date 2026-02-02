package com.example.uber3.network;

import com.example.uber3.network.model.ProfileResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Body;
import com.example.uber3.network.model.UpdateProfileRequest;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;



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


}
