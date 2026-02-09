package com.example.uber3.network.service;

import android.net.Uri;
import android.content.Context;
import com.example.uber3.network.model.DriverProfileChangeRequestDto;
import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.ApiService;
import com.example.uber3.network.model.ProfileResponse;
import com.example.uber3.network.model.UpdateProfileRequest;
import com.example.uber3.network.model.user.BlockStatusDto;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Callback;

public class ProfileService {

    private final ApiService api;


    public ProfileService(Context context) {
        api = ApiClient
                .getClient(context)
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

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[4096];

            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            byte[] bytes = buffer.toByteArray();
            buffer.close();
            inputStream.close();


            RequestBody requestFile =
                    RequestBody.create(
                            bytes,
                            MediaType.parse(mimeType)
                    );

            MultipartBody.Part body =
                    MultipartBody.Part.createFormData(
                            "file",
                            "profile_" + System.currentTimeMillis() + ".jpg",
                            requestFile
                    );

            api.uploadProfileImage(body)
                    .enqueue(callback);

        } catch (Exception e) {
            callback.onFailure(null, e);
        }
    }

    public void submitDriverChangeRequest(
            DriverProfileChangeRequestDto dto,
            Callback<Void> callback
    ) {
        api.submitDriverChangeRequest(dto).enqueue(callback);
    }

    public void getBlockStatus(Callback<BlockStatusDto> cb) {
        api.getBlockStatus().enqueue(cb);
    }
}
