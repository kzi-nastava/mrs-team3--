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
import retrofit2.Callback;

public class ProfileService {

    private final ApiService api;

    public ProfileService(String token) {
        api = ApiClient
                .getClient(token)
                .create(ApiService.class);
    }

    public void loadProfile(Callback<ProfileResponse> callback) {
        api.getMyProfile().enqueue(callback);
    }

    public void updateProfile(UpdateProfileRequest req,
                              Callback<Void> callback) {
        api.updateProfile(req).enqueue(callback);
    }

    public void uploadImage(Context context,
                            Uri uri,
                            Callback<String> callback) {

        try {
            String mimeType =
                    context.getContentResolver().getType(uri);

            if (mimeType == null) {
                mimeType = "image/jpeg";
            }

            InputStream inputStream =
                    context.getContentResolver()
                            .openInputStream(uri);

            if (inputStream == null) {
                callback.onFailure(null,
                        new Exception("Cannot open stream"));
                return;
            }

            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            RequestBody requestFile =
                    RequestBody.create(bytes,
                            MediaType.parse(mimeType));

            MultipartBody.Part body =
                    MultipartBody.Part.createFormData(
                            "file",
                            "upload.jpg",
                            requestFile
                    );

            api.uploadProfileImage(body)
                    .enqueue(callback);

        } catch (Exception e) {
            callback.onFailure(null, e);
        }
    }


}
