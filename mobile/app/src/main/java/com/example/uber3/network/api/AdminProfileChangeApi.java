package com.example.uber3.network.api;

import com.example.uber3.network.model.AdminDriverProfileChangeRequestDetailsDto;
import com.example.uber3.network.model.AdminDriverProfileChangeRequestDto;
import com.example.uber3.network.model.AdminProfileChangeDecisionDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Body;

public interface AdminProfileChangeApi {

    @GET("/api/admin/profile-change-requests")
    Call<List<AdminDriverProfileChangeRequestDto>> getAll();

    @GET("/api/admin/profile-change-requests/pending")
    Call<List<AdminDriverProfileChangeRequestDto>> getPending();

    @POST("/api/admin/profile-change-requests/{id}/decision")
    Call<Void> decide(
            @Path("id") Long id,
            @Body AdminProfileChangeDecisionDto decision
    );

    @GET("/api/admin/profile-change-requests/{id}")
    Call<AdminDriverProfileChangeRequestDetailsDto> getDetails(
            @Path("id") Long id
    );
}
