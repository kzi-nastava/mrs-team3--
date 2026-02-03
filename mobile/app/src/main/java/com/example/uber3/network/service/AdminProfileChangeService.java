package com.example.uber3.network.service;

import android.content.Context;

import com.example.uber3.network.api.ApiClient;
import com.example.uber3.network.api.AdminProfileChangeApi;
import com.example.uber3.network.model.AdminDriverProfileChangeRequestDetailsDto;
import com.example.uber3.network.model.AdminDriverProfileChangeRequestDto;
import com.example.uber3.network.model.AdminProfileChangeDecisionDto;

import java.util.List;

import retrofit2.Callback;

public class AdminProfileChangeService {

    private final AdminProfileChangeApi api;

    public AdminProfileChangeService(Context ctx) {
        api = ApiClient
                .getClient(ctx)
                .create(AdminProfileChangeApi.class);
    }

    public void getAll(
            Callback<List<AdminDriverProfileChangeRequestDto>> cb
    ) {
        api.getAll().enqueue(cb);
    }

    public void decide(
            Long id,
            AdminProfileChangeDecisionDto dto,
            Callback<Void> cb
    ) {
        api.decide(id, dto).enqueue(cb);
    }

    public void getDetails(
            Long id,
            Callback<AdminDriverProfileChangeRequestDetailsDto> cb
    ) {
        api.getDetails(id).enqueue(cb);
    }
}
