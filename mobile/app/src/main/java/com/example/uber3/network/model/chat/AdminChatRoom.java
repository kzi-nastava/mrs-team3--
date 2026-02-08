package com.example.uber3.network.model.chat;

public class AdminChatRoom {
    public Long userId;
    public String userName;

    public AdminChatRoom(Long userId, String userName){
        this.userId = userId;
        this.userName = userName;
    }
}