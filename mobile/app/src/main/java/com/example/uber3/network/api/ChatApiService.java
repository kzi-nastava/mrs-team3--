package com.example.uber3.network.api;


import com.example.uber3.network.model.ChatRoomDto;
import com.example.uber3.network.model.chat.AdminDto;
import com.example.uber3.network.model.chat.ChatMessage;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ChatApiService {

    @GET("/api/chat/admin/rooms")
    Call<List<ChatRoomDto>> getAdminRooms(
            @Query("adminId") Long adminId
    );

    @GET("api/chat/history")
    Call<List<ChatMessage>> getHistory(
            @Query("user1") Long u1,
            @Query("user2") Long u2
    );

    @GET("/api/chat/admin")
    Call<AdminDto> getFirstAdmin();
}