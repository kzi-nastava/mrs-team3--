package com.example.uber3.network.api;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ORSRetrofitClient {

    private static final String BASE_URL =
            "https://api.openrouteservice.org/";

    private static Retrofit retrofit;

    public static Retrofit getInstance() {

        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(
                            ScalarsConverterFactory.create()
                    )
                    .build();
        }

        return retrofit;
    }
}
