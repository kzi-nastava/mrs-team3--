package com.example.uber3.network.service;

import android.net.Uri;
import android.content.Context;

import com.example.uber3.network.ApiClient;
import com.example.uber3.network.ApiService;
import com.example.uber3.network.model.ProfileResponse;
import com.example.uber3.network.model.UpdateProfileRequest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

public class ProfileService {

    private final ApiService api;

    public ProfileService(String token) {
        api = ApiClient
                .getClient(token)
                .create(ApiService.class);
    }

    // LOAD PROFILE
    public void loadProfile(Callback<ProfileResponse> callback) {
        api.getMyProfile().enqueue(callback);
    }

    // UPDATE PROFILE
    public void updateProfile(UpdateProfileRequest req,
                              Callback<Void> callback) {
        api.updateProfile(req).enqueue(callback);
    }

    // UPLOAD IMAGE
    public void uploadImage(Context context,
                            Uri uri,
                            Callback<String> callback) {

        try {
            InputStream inputStream =
                    context.getContentResolver().openInputStream(uri);

            ByteArrayOutputStream buffer =
                    new ByteArrayOutputStream();

            byte[] data = new byte[4096];
            int nRead;

            while ((nRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, nRead);
            }

            byte[] bytes = buffer.toByteArray();

            RequestBody requestFile =
                    RequestBody.create(
                            bytes,
                            MediaType.parse("image/*")
                    );

            MultipartBody.Part body =
                    MultipartBody.Part.createFormData(
                            "file",
                            "profile.jpg",
                            requestFile
                    );

            api.uploadProfileImage(body)
                    .enqueue(callback);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
