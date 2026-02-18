package com.example.uber3.network.model.notification;

import com.google.gson.annotations.SerializedName;

public class Notification {

    public long id;

    public String type;

    public String message;

    public boolean isRead;

    public String createdAt;

    public Long relatedEntityId;

    public String recipientRole;

    public Notification() {}
}