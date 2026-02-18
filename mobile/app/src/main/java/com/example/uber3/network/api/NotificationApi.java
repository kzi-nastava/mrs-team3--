package com.example.uber3.network.api;

import com.example.uber3.network.model.notification.Notification;
import com.example.uber3.network.model.notification.NotificationCount;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface NotificationApi {

    @GET("api/notifications")
    Call<List<Notification>> getNotifications(
            @Header("Authorization") String bearerToken
    );

    @GET("api/notifications/unread/count")
    Call<NotificationCount> getUnreadCount(
            @Header("Authorization") String bearerToken
    );

    @PUT("api/notifications/{id}/read")
    Call<Notification> markAsRead(
            @Header("Authorization") String bearerToken,
            @Path("id") long notificationId
    );

    @PUT("api/notifications/read-all")
    Call<Void> markAllAsRead(
            @Header("Authorization") String bearerToken
    );

    @DELETE("api/notifications/{id}")
    Call<Void> deleteNotification(
            @Header("Authorization") String bearerToken,
            @Path("id") long notificationId
    );
}