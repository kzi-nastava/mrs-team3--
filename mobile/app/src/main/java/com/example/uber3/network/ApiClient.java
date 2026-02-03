package com.example.uber3.network;

import android.content.Context;

import com.example.uber3.network.manager.TokenManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:8080/";

    public static Retrofit getClient(Context context) {

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {

                    String token = TokenManager.getToken(context);

                    Request.Builder requestBuilder =
                            chain.request().newBuilder();

                    if (token != null) {
                        requestBuilder.addHeader(
                                "Authorization",
                                "Bearer " + token
                        );
                    }

                    return chain.proceed(requestBuilder.build());
                })
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
    }
}
